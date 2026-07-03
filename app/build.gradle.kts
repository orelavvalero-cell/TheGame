plugins {
    id("com.android.application")
}

android {
    namespace = "com.valeriy.thegame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.valeriy.thegame"
        minSdk = 23
        targetSdk = 35
        versionCode = 34
        versionName = "0.1.0034"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
