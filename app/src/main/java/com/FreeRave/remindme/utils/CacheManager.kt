package com.FreeRave.remindme.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "CacheManager"
private const val CACHE_DIR = "location_cache"
private const val GEOCODING_CACHE_FILE = "geocoding_cache.json"
private const val API_CACHE_FILE = "api_cache.json"
private const val LOCATION_HISTORY_FILE = "location_history.json"
private const val MAX_CACHE_SIZE_MB = 50L
private const val MAX_LOCATION_HISTORY = 20

/**
 * Comprehensive cache manager for geolocation and API data
 * Provides thread-safe caching with automatic cleanup and size management
 */
class CacheManager(private val context: Context) {

    private val gson = Gson()
    private val cacheMutex = Mutex()
    private val cacheDir: File = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }

    // Cache data structures
    private var geocodingCache = mutableMapOf<String, GeocodingCacheEntry>()
    private var apiCache = mutableMapOf<String, ApiCacheEntry>()
    private var locationHistory = mutableListOf<LocationHistoryEntry>()

    init {
        loadCaches()
        // Schedule periodic cleanup
        cleanupExpiredEntries()
    }

    // Geocoding Cache Methods
    suspend fun getGeocodingResult(query: String): GeocodingCacheEntry? = cacheMutex.withLock {
        val entry = geocodingCache[query]
        if (entry != null && !entry.isExpired()) {
            Log.d(TAG, "Geocoding cache hit for: $query")
            return entry
        }
        // Remove expired entry
        if (entry?.isExpired() == true) {
            geocodingCache.remove(query)
        }
        null
    }

    suspend fun putGeocodingResult(query: String, latitude: Double, longitude: Double, address: String) = cacheMutex.withLock {
        val entry = GeocodingCacheEntry(
            latitude = latitude,
            longitude = longitude,
            address = address,
            timestamp = System.currentTimeMillis()
        )
        geocodingCache[query] = entry
        saveGeocodingCache()
        Log.d(TAG, "Cached geocoding result for: $query")
    }

    // API Cache Methods
    suspend fun getApiResponse(url: String): ApiCacheEntry? = cacheMutex.withLock {
        val entry = apiCache[url]
        if (entry != null && !entry.isExpired()) {
            Log.d(TAG, "API cache hit for: $url")
            return entry
        }
        // Remove expired entry
        if (entry?.isExpired() == true) {
            apiCache.remove(url)
        }
        null
    }

    suspend fun putApiResponse(url: String, response: String, ttlMinutes: Long = 60) = cacheMutex.withLock {
        val entry = ApiCacheEntry(
            response = response,
            timestamp = System.currentTimeMillis(),
            ttlMillis = TimeUnit.MINUTES.toMillis(ttlMinutes)
        )
        apiCache[url] = entry
        saveApiCache()
        Log.d(TAG, "Cached API response for: $url")
    }

    // Location History Methods
    suspend fun getLocationHistory(): List<LocationHistoryEntry> = cacheMutex.withLock {
        locationHistory.toList()
    }

    suspend fun addLocationToHistory(latitude: Double, longitude: Double, address: String?, name: String? = null) = cacheMutex.withLock {
        val entry = LocationHistoryEntry(
            latitude = latitude,
            longitude = longitude,
            address = address,
            name = name,
            timestamp = System.currentTimeMillis(),
            usageCount = 1
        )

        // Check if location already exists, increment usage count
        val existingIndex = locationHistory.indexOfFirst {
            it.latitude == latitude && it.longitude == longitude
        }

        if (existingIndex >= 0) {
            val existing = locationHistory[existingIndex]
            locationHistory[existingIndex] = existing.copy(
                usageCount = existing.usageCount + 1,
                timestamp = System.currentTimeMillis()
            )
        } else {
            locationHistory.add(0, entry) // Add to beginning
            // Keep only recent entries
            if (locationHistory.size > MAX_LOCATION_HISTORY) {
                locationHistory = locationHistory.take(MAX_LOCATION_HISTORY).toMutableList()
            }
        }

        saveLocationHistory()
        Log.d(TAG, "Added location to history: ${address ?: "$latitude,$longitude"}")
    }

    // Cache Management Methods
    suspend fun clearAllCaches() = cacheMutex.withLock {
        geocodingCache.clear()
        apiCache.clear()
        locationHistory.clear()
        deleteCacheFiles()
        Log.d(TAG, "Cleared all caches")
    }

    suspend fun getCacheStats(): CacheStats = cacheMutex.withLock {
        val totalSize = calculateCacheSize()
        CacheStats(
            geocodingEntries = geocodingCache.size,
            apiEntries = apiCache.size,
            locationHistoryEntries = locationHistory.size,
            totalSizeBytes = totalSize,
            totalSizeMB = totalSize / (1024.0 * 1024.0)
        )
    }

    private fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()

        // Clean geocoding cache
        geocodingCache.entries.removeIf { it.value.isExpired() }

        // Clean API cache
        apiCache.entries.removeIf { it.value.isExpired() }

        // Clean location history (keep only recent and frequently used)
        locationHistory = locationHistory
            .filter { currentTime - it.timestamp < TimeUnit.DAYS.toMillis(30) } // Keep last 30 days
            .sortedByDescending { it.usageCount * 1000 + it.timestamp } // Sort by usage and recency
            .take(MAX_LOCATION_HISTORY)
            .toMutableList()

        // Check total cache size and evict if needed
        val totalSize = calculateCacheSize()
        if (totalSize > MAX_CACHE_SIZE_MB * 1024 * 1024) {
            evictOldEntries()
        }
    }

    private fun evictOldEntries() {
        // Evict oldest geocoding entries (keep 50%)
        val geocodingToKeep = geocodingCache.size / 2
        if (geocodingToKeep > 0) {
            geocodingCache = geocodingCache.entries
                .sortedBy { it.value.timestamp }
                .takeLast(geocodingToKeep)
                .associate { it.key to it.value }
                .toMutableMap()
        }

        // Evict oldest API entries (keep 30%)
        val apiToKeep = (apiCache.size * 0.3).toInt()
        if (apiToKeep > 0) {
            apiCache = apiCache.entries
                .sortedBy { it.value.timestamp }
                .takeLast(apiToKeep)
                .associate { it.key to it.value }
                .toMutableMap()
        }
    }

    private fun calculateCacheSize(): Long {
        return try {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    // Persistence Methods
    private fun loadCaches() {
        try {
            // Load geocoding cache
            val geocodingFile = File(cacheDir, GEOCODING_CACHE_FILE)
            if (geocodingFile.exists()) {
                val json = geocodingFile.readText()
                val type = object : TypeToken<Map<String, GeocodingCacheEntry>>() {}.type
                val cachedData: Map<String, GeocodingCacheEntry>? = gson.fromJson(json, type)
                geocodingCache = (cachedData ?: emptyMap()).toMutableMap()
            }

            // Load API cache
            val apiFile = File(cacheDir, API_CACHE_FILE)
            if (apiFile.exists()) {
                val json = apiFile.readText()
                val type = object : TypeToken<Map<String, ApiCacheEntry>>() {}.type
                val cachedData: Map<String, ApiCacheEntry>? = gson.fromJson(json, type)
                apiCache = (cachedData ?: emptyMap()).toMutableMap()
            }

            // Load location history
            val historyFile = File(cacheDir, LOCATION_HISTORY_FILE)
            if (historyFile.exists()) {
                val json = historyFile.readText()
                val type = object : TypeToken<List<LocationHistoryEntry>>() {}.type
                val cachedData: List<LocationHistoryEntry>? = gson.fromJson(json, type)
                locationHistory = (cachedData ?: emptyList()).toMutableList()
            }

            Log.d(TAG, "Loaded caches: ${geocodingCache.size} geocoding, ${apiCache.size} API, ${locationHistory.size} history entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load caches", e)
        }
    }

    private fun saveGeocodingCache() {
        try {
            val json = gson.toJson(geocodingCache)
            File(cacheDir, GEOCODING_CACHE_FILE).writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save geocoding cache", e)
        }
    }

    private fun saveApiCache() {
        try {
            val json = gson.toJson(apiCache)
            File(cacheDir, API_CACHE_FILE).writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save API cache", e)
        }
    }

    private fun saveLocationHistory() {
        try {
            val json = gson.toJson(locationHistory)
            File(cacheDir, LOCATION_HISTORY_FILE).writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save location history", e)
        }
    }

    private fun deleteCacheFiles() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete cache files", e)
        }
    }
}

// Data Classes
data class GeocodingCacheEntry(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val timestamp: Long
) {
    fun isExpired(): Boolean {
        // Expire after 48 hours
        return System.currentTimeMillis() - timestamp > TimeUnit.HOURS.toMillis(48)
    }
}

data class ApiCacheEntry(
    val response: String,
    val timestamp: Long,
    val ttlMillis: Long
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > ttlMillis
    }
}

data class LocationHistoryEntry(
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val name: String?,
    val timestamp: Long,
    val usageCount: Int
)

data class CacheStats(
    val geocodingEntries: Int,
    val apiEntries: Int,
    val locationHistoryEntries: Int,
    val totalSizeBytes: Long,
    val totalSizeMB: Double
)
