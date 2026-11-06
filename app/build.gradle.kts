plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val repo = System.getenv("GITHUB_REPOSITORY") ?: "eniigma07/Drama"

android {
    namespace = "com.drama.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.drama.app"
        minSdk = 29
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
        buildConfigField("String", "CATALOG_URL", "\"https://github.com/$repo/releases/download/drama-catalog/catalog.json\"")
    }

    signingConfigs {
        create("staly") {
            // Stały klucz: nowe wersje instalują się na starych bez utraty listy i postępów
            storeFile = file("drama.jks")
            storePassword = "dramadrama"
            keyAlias = "drama"
            keyPassword = "dramadrama"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("staly")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    // Oficjalny odtwarzacz YouTube (IFrame) w aplikacji
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.1.0")
}
