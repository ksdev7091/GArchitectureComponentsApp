import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // Navigation の型安全ルート(@Serializable)に必要
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.archnotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.archnotes"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // リモートデータソースの実装を flavor で切り替える(公式サンプル Now in Android と同じ方式)。
    // - demo: ネットワークに出ない疑似リモート(FakeNoteRemoteDataSource)。学習・UI 確認用
    // - prod: Retrofit で公開 API(JSONPlaceholder)に接続する本物のリモート
    // どちらを使うかは src/demo と src/prod にある RemoteModule(Hilt)が決める。
    flavorDimensions += "environment"
    productFlavors {
        create("demo") {
            dimension = "environment"
            applicationIdSuffix = ".demo"
            buildConfigField("String", "API_BASE_URL", "\"http://localhost/\"") // demo では使わない
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://jsonplaceholder.typicode.com/\"")
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
    buildFeatures {
        compose = true
        buildConfig = true // flavor ごとの API_BASE_URL を BuildConfig に出す
    }
}

dependencies {
    // --- UI層: Compose / Lifecycle / Navigation ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose) // collectAsStateWithLifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // --- Data層: Room(2.7 からコルーチン/Flow 対応は room-runtime に含まれる) ---
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // --- Data層: ネットワーク(Retrofit + OkHttp + kotlinx.serialization) ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // --- 非同期: Coroutines ---
    implementation(libs.kotlinx.coroutines.android)

    // --- DI: Hilt ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // --- テスト ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver) // Retrofit 実装を本物の HTTP で検証する
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
}
