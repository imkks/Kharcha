#!/bin/bash
MODULES=(
    "core:model" "core:database" "core:common" "core:designsystem"
    "feature:onboarding" "feature:home" "feature:transactions" "feature:review"
    "feature:insights" "feature:accounts" "feature:settings"
    "engine:sms" "engine:parser" "engine:classifier" "engine:categorizer" "engine:recurring"
)

# App module is special
mkdir -p app/src/main/java/com/kharcha
mkdir -p app/src/main/res
mkdir -p app/src/test/java
mkdir -p app/src/androidTest/java

for module in "${MODULES[@]}"; do
    path=$(echo $module | tr ':' '/')
    mkdir -p $path/src/main/java/com/kharcha/${path//\//.}
    mkdir -p $path/src/main/res
    mkdir -p $path/src/test/java
    mkdir -p $path/src/androidTest/java

    cat << BUILD_GRADLE > $path/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    // hilt, ksp depending on the module later
}

android {
    namespace = "com.kharcha.\${path//\\//.}"
    compileSdk = 34 // Assuming target SDK

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
BUILD_GRADLE

    cat << MANIFEST > $path/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
MANIFEST

    # Create proguard file
    touch $path/proguard-rules.pro

    # .gitignore per module
    cat << GITIGNORE > $path/.gitignore
/build
GITIGNORE
done
