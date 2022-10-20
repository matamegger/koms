plugins {
    kotlin("multiplatform") version "1.7.20"
    id("com.android.library")
    id("kotlin-android-extensions")
    id("maven-publish")
}

group = "sh.uffle"
version = "0.1"

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    android()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.benasher44:uuid:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            kotlin.srcDir("src/commonAndroidJvmMain/kotlin")
            dependencies {
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13")
            }
        }
        val jvmMain by getting {
            kotlin.srcDir("src/commonAndroidJvmMain/kotlin")
            dependencies {

            }
        }
        val jvmTest by getting
    }
}

android {
    compileSdkVersion(33)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(33)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}