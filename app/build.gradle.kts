plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.vikram.expressiveisland"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vikram.expressiveisland"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "0.1.5"
    }

    val releaseStoreFile = providers.gradleProperty("releaseStoreFile").orNull
        ?: System.getenv("EXPRESSIVE_RELEASE_STORE_FILE")
    val releaseStorePassword = providers.gradleProperty("releaseStorePassword").orNull
        ?: System.getenv("EXPRESSIVE_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orNull
        ?: System.getenv("EXPRESSIVE_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orNull
        ?: System.getenv("EXPRESSIVE_RELEASE_KEY_PASSWORD")

    if (releaseStoreFile != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null
    ) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseStoreFile != null && releaseStorePassword != null &&
                releaseKeyAlias != null && releaseKeyPassword != null
            ) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.lottie.compose)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
