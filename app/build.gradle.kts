plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace  = "org.wpfy.carlauncher"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId  = "com.rouh.auto"
        minSdk         = 28
        targetSdk      = 36
        // CI run numbers increase on each build; local builds retain the development baseline.
        versionCode    = (System.getenv("ROUH_BUILD_NUMBER")?.toIntOrNull() ?: 10)
        versionName    = "1.2"
    }

    // Release signing is enabled only when CI provides all required secrets.
    val releaseKeystore = System.getenv("ROUH_KEYSTORE_PATH")
    val releasePassword = System.getenv("ROUH_KEYSTORE_PASSWORD")
    val releaseAlias = System.getenv("ROUH_KEY_ALIAS")
    val hasReleaseSigning = !releaseKeystore.isNullOrBlank() &&
        !releasePassword.isNullOrBlank() && !releaseAlias.isNullOrBlank()

    signingConfigs {
        if (hasReleaseSigning) {
            create("rouhRelease") {
                storeFile = file(releaseKeystore!!)
                storePassword = releasePassword
                keyAlias = releaseAlias
                keyPassword = releasePassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            // Default signing config for normal device testing (restores app visibility)
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("rouhRelease")
            }
            isMinifyEnabled = false
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
