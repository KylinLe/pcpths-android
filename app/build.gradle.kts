import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 正式版签名：密码存在 keystore.properties（已加入 .gitignore，切勿删除）
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    namespace = "com.pcpths.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pcpths.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.3"
    }

    // 双渠道（2026-11-21 起）：
    // prod = 正式版 com.pcpths.app（应用名「普中摄影社」）
    // beta = 测试版 com.pcpths.test（应用名「普中摄影社·测试」）——包名完全不同，与正式版共存，测试期间不用卸载正式版
    flavorDimensions += "channel"
    productFlavors {
        create("prod") {
            dimension = "channel"
            applicationId = "com.pcpths.app"
        }
        create("beta") {
            dimension = "channel"
            applicationId = "com.pcpths.test"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 找不到 keystore.properties 时退回 debug 签名（仅本地开发兜底，发布必须用正式签名）
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
    testImplementation("junit:junit:4.13.2")
}
