import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kharcha"
    compileSdk = 36

    val keystorePropFile = rootProject.file("keystore.properties").takeIf { it.exists() }
        ?: rootProject.file("key.properties").takeIf { it.exists() }
    val signingProps = Properties().apply {
        if (keystorePropFile != null) {
            load(FileInputStream(keystorePropFile))
        }
    }

    val releaseKeystorePath = signingProps.getProperty("storeFile")
        ?: (project.findProperty("KEYSTORE_PATH") as? String)
        ?: System.getenv("ANDROID_KEYSTORE_PATH")
        ?: System.getenv("KEYSTORE_PATH")
    val releaseKeystorePassword = signingProps.getProperty("storePassword")
        ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)
        ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")
    val releaseKeyAlias = signingProps.getProperty("keyAlias")
        ?: (project.findProperty("KEY_ALIAS") as? String)
        ?: System.getenv("ANDROID_KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
    val releaseKeyPassword = signingProps.getProperty("keyPassword")
        ?: (project.findProperty("KEY_PASSWORD") as? String)
        ?: System.getenv("ANDROID_KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")
        ?: releaseKeystorePassword
    val resolvedStoreFile = releaseKeystorePath?.let { path ->
        val candidate = file(path)
        if (candidate.exists()) candidate else rootProject.file(path)
    }

    val hasReleaseSigning = listOf(
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() } && resolvedStoreFile?.exists() == true

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = resolvedStoreFile
                storePassword = requireNotNull(releaseKeystorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    defaultConfig {
        applicationId = "com.evertrail.kharcha"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("appVersionName").orNull ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // matches kotlin 1.9.24
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Needed for the system BiometricPrompt used by the optional app lock.
    implementation(libs.androidx.biometric)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:review"))
    implementation(project(":feature:insights"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:settings"))
    implementation(project(":engine:sms"))
    implementation(project(":engine:parser"))
    implementation(project(":engine:classifier"))
    implementation(project(":engine:categorizer"))
    implementation(project(":engine:recurring"))

    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(project(":engine:sms"))
}
