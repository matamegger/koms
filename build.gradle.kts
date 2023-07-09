import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
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
        jvmToolchain(11)
    }

    android {
        jvmToolchain(11)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.uuid)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.framework.engine)
                implementation(libs.strikt.core)
                implementation(libs.turbine)
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val androidMain by getting
        val androidUnitTest by getting
        val commonJvmMain by creating {
            dependsOn(commonMain)
            jvmMain.dependsOn(this)
            androidMain.dependsOn(this)
        }
        val commonJvmTest by creating {
            dependsOn(commonTest)
            jvmTest.dependsOn(this)
            androidUnitTest.dependsOn(this)
            dependencies {
                implementation(libs.kotest.junit5)
                implementation(libs.junit.engine)
            }
        }
    }
}

android {
    namespace = "sh.uffle.koms"
    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = 26
    }
}

ktlint {
    outputColorName.set("RED")
    version.set(libs.versions.ktlint)
}

allprojects {
    project.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            allWarningsAsErrors = true
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = true
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
        )
        exceptionFormat = TestExceptionFormat.FULL
    }
}

koverReport {
    defaults {
        mergeWith("release")
    }
}
