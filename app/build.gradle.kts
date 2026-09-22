import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
}

// 以 git 提交次数作为 versionCode，版本名采用 "<count>-git-<shortHash>" 格式
fun gitCommitCount(): Int {
    return try {
        val out = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = out
        }.assertNormalExitValue()
        out.toString().trim().toInt()
    } catch (e: Exception) {
        1
    }
}

fun gitCommitCountValue(): Int = gitCommitCount()

fun gitShortHash(): String {
    return try {
        val out = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = out
        }.assertNormalExitValue()
        out.toString().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

val gitVersionCode = gitCommitCountValue()
val gitVersionName = "$gitVersionCode-git-${gitShortHash()}"

android {
    namespace = "com.fengyi.screenrecord"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fengyi.screenrecord"
        minSdk = 26
        targetSdk = 35
        versionCode = gitVersionCode
        versionName = gitVersionName
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/release.jks")
            val ksp = System.getenv("KEYSTORE_PASSWORD") ?: "ScreenRecord2026"
            storePassword = ksp
            keyAlias = "screenrecord"
            keyPassword = ksp
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
}
