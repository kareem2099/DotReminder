import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.ksp)
    kotlin("plugin.parcelize")

}

// Function to load properties from local.properties
fun getApiKey(propertyKey: String): String {
    val properties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(FileInputStream(localPropertiesFile))
        return properties.getProperty(propertyKey, "")
    }
    return ""
}

android {
    namespace = "com.FreeRave.remindme"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.FreeRave.remindme"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "0.0.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiApiKey = getApiKey("GEMINI_API_KEY")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")



        // Custom LLM configuration
        val llmApiUrl = getApiKey("LLM_API_URL")
        buildConfigField("String", "LLM_API_URL", "\"$llmApiUrl\"")
        val llmModelName = getApiKey("LLM_MODEL_NAME")
        buildConfigField("String", "LLM_MODEL_NAME", "\"$llmModelName\"")

        // Ads configuration
        val bannerAdUnitId = getApiKey("BANNER_AD_UNIT_ID")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${bannerAdUnitId.ifEmpty { "ca-app-pub-3186351063839089/9630737183" }}\"")

        val interstitialAdUnitId = getApiKey("INTERSTITIAL_AD_UNIT_ID")
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${interstitialAdUnitId.ifEmpty { "ca-app-pub-3186351063839089/4539844990" }}\"")

        val appOpenAdUnitId = getApiKey("APP_OPEN_AD_UNIT_ID")
        buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"${appOpenAdUnitId.ifEmpty { "ca-app-pub-3186351063839089/2600077363" }}\"")

        val rewardedAdUnitId = getApiKey("REWARDED_AD_UNIT_ID")
        buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${rewardedAdUnitId.ifEmpty { "ca-app-pub-3186351063839089/8973914022" }}\"")

        // Google Maps API key
        val mapsApiKey = getApiKey("GOOGLE_MAPS_API_KEY")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val storeFilePath = getApiKey("MYAPP_RELEASE_STORE_FILE")
            val storePass = getApiKey("MYAPP_RELEASE_STORE_PASSWORD")
            val keyAlias = getApiKey("MYAPP_RELEASE_KEY_ALIAS")
            val keyPass = getApiKey("MYAPP_RELEASE_KEY_PASSWORD")

            if (storeFilePath.isNotEmpty() && storePass.isNotEmpty() && keyAlias.isNotEmpty() && keyPass.isNotEmpty()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePass
                this.keyAlias = keyAlias
                keyPassword = keyPass
            } else {
                println("WARNING: Missing release keystore properties in local.properties.")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    ndkVersion = "29.0.13113456"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    implementation(libs.compose.calendar)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    implementation(libs.play.services.identity)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.maps)
    implementation(libs.maps.ktx)
    implementation(libs.user.messaging.platform)
    implementation(libs.country.picker.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics.ktx)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.javax.inject)
    implementation(libs.billing.ktx)
    implementation(libs.androidx.compose.ui)
    ksp(libs.hilt.compiler)
    kspAndroidTest(libs.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material3) // M3 is already included via bom, but explicit declaration is fine
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.foundation.layout)

    implementation(libs.facebook.android.sdk)
    implementation(libs.libphonenumber)
    implementation(libs.zxing.core)
    implementation(libs.natty)
    implementation(libs.google.ai.generativeai)
    implementation(libs.gson)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.metadata.jvm)

    // --- Google Play In-App Updates ---
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.appcompat)
}
