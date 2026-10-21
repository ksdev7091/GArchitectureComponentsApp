import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.legacytodo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.legacytodo.step0"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    // 昔ながらの構成: AppCompat だけ。Coroutines も ViewModel も使っていない。
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}
