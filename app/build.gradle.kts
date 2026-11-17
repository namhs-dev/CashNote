import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.aboutlibraries.android)
    // app/google-services.json이 있어야 한다(커밋하지 않음, CI는 Secret으로 만든다)
    alias(libs.plugins.google.services)
    // release 빌드의 R8 매핑 파일을 Crashlytics에 올려 난독화된 스택트레이스를 복원한다
    alias(libs.plugins.firebase.crashlytics)
    // Play 스토어 스크린샷·그래픽 이미지를 JVM 테스트(Robolectric)로 렌더링한다 (#61)
    alias(libs.plugins.roborazzi)
}

// 환율 API 키. CI는 환경 변수(GitHub Secrets), 로컬은 local.properties에서 읽는다.
// 둘 다 없으면 빈 문자열로 빌드는 되게 하고, API 호출 시점에 처리한다(#36)
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val exchangeRateApiKey: String =
    providers.environmentVariable("EXCHANGE_RATE_API_KEY").orNull
        ?: localProperties.getProperty("EXCHANGE_RATE_API_KEY")
        ?: ""

// release 업로드 키 서명 정보. 루트의 keystore.properties(커밋하지 않음)에서 읽는다.
// 파일이 없으면 서명 없이 빌드되게 둔다(CI는 release를 빌드하지 않는다) (#62)
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "com.namhs.cashnote"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.namhs.cashnote"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "EXCHANGE_RATE_API_KEY", "\"$exchangeRateApiKey\"")
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Crashlytics 수집 여부. AndroidManifest의 firebase_crashlytics_collection_enabled에 들어간다.
        // 개발 중 크래시가 콘솔에 섞이지 않도록 debug는 끈다
        debug {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        release {
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
            signingConfig = signingConfigs.findByName("release")
            // R8로 앱 코드와 모든 라이브러리를 줄이고 난독화한다(packageScope 기본값 "**").
            // Retrofit·kotlinx-serialization·Room·Hilt·WorkManager는 라이브러리가 함께 배포하는 keep 규칙으로 충분해
            // 앱 전용 규칙은 두지 않는다 (#44: release 빌드를 실기기에서 API·DB·화면 이동까지 확인)
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Robolectric이 앱 리소스(문자열·drawable·테마)를 읽어 화면을 그릴 수 있게 한다
        unitTests.isIncludeAndroidResources = true
    }
}

// Room 스키마를 버전별 JSON으로 내보내 Git에 커밋한다. migration 작성·테스트의 기준이 된다.
room {
    schemaDirectory("$projectDir/schemas")
}

// 스토어 이미지 렌더링(store 패키지)은 recordRoborazziDebug로 만들 때만 돌린다.
// 일반 단위 테스트·CI에서 Robolectric용 Android 런타임을 내려받지 않게 제외한다
val isRoborazziRun = gradle.startParameter.taskNames.any { it.contains("Roborazzi") }
tasks.withType<Test>().configureEach {
    if (!isRoborazziRun) exclude("**/store/**")
}

ktlint {
    version.set(libs.versions.ktlintEngine.get())
    android.set(true)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(platform(libs.okhttp.bom))
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
