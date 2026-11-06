import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// =====================================================================
//  SETĂRILE TALE — modifică doar aici
// =====================================================================
// ID-urile de mai jos sunt ID-urile de TEST oficiale Google AdMob.
// Înainte de publicare, înlocuiește-le cu ID-urile din contul tău AdMob.
val admobAppId = "ca-app-pub-3940256099942544~3347511713"
val admobBannerId = "ca-app-pub-3940256099942544/9214589741"
val admobInterstitialId = "ca-app-pub-3940256099942544/1033173712"

// Linkul către fișierul config/app-config.json (controlează popup-ul și reclamele
// fără să republici aplicația). Exemplu pentru GitHub:
// https://raw.githubusercontent.com/UTILIZATOR/CalmLoop/main/config/app-config.json
val remoteConfigUrl = "https://raw.githubusercontent.com/thegreatappbuilder/CalmLoop/main/config/app-config.json"

// Linkul către politica de confidențialitate (obligatoriu în Play Store).
val privacyPolicyUrl = ""
// =====================================================================

// Cheia de semnare: din variabile de mediu (GitHub Actions) sau din keystore.properties (local)
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: keystoreProps.getProperty(prop)

val releaseStoreFile = signingValue("KEYSTORE_FILE", "storeFile")
val hasReleaseKey = releaseStoreFile != null && rootProject.file(releaseStoreFile).exists()

android {
    namespace = "com.calmloop.sounds"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.calmloop.sounds"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "REMOTE_CONFIG_URL", "\"$remoteConfigUrl\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyPolicyUrl\"")
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = signingValue("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
