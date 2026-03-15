package com.FreeRave.remindme.utils

object Constants {
    // Android client ID
    const val GOOGLE_CLIENT_ID = "12141681876-15dblo0pebaf2m8c5f9o4j2u8c6i0bu0.apps.googleusercontent.com" // Updated to match google-services.json (type 1 client)
    
    // Web client ID (required for server verification)
    // This MUST match the "Web client ID" from Firebase Console > Authentication > Sign-in method > Google > Web SDK configuration
    const val GOOGLE_WEB_CLIENT_ID = "12141681876-qr8p7spogfpkqb7q49o3ohas2l5cdf42.apps.googleusercontent.com"
    
    // Firebase configuration
    const val FIREBASE_PROJECT_ID = "remindme-56f27"
}
