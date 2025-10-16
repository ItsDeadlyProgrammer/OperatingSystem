import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    // CRITICAL: Ensure the Compose Multiplatform plugin is applied
    id("org.jetbrains.compose") version "1.9.0"

    // FIX: Required for Compose Multiplatform with Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    // --- Target Configuration ---
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }


    iosArm64()
    iosSimulatorArm64()

    // Desktop/JVM target (Maps to the jvmMain source set)
    jvm()

    // Web targets
    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()

    }

    // --- Source Sets and Dependencies ---
    sourceSets {
        // --- Common Dependencies (Shared Logic & UI) ---
        commonMain.dependencies {
            // Compose Multiplatform Dependencies
            implementation(compose.runtime)        // Core Compose runtime
            implementation(compose.foundation)     // Low-level UI components
            implementation(compose.material3)       // Modern Material Design components (buttons, text fields)

            // Asynchronous Operations (KMP version)
            implementation(libs.kotlinx.coroutines.core.v181)
            implementation(compose.materialIconsExtended)

        }

        // --- Desktop Specific Dependencies (jvmMain is the desktop equivalent) ---
        // 'jvm()' targets are automatically mapped to the 'jvmMain' source set.
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs) // Needed for desktop app entry point
            }
        }

        // --- Android Specific Dependencies ---
        val androidMain by getting {
            // Android dependencies usually managed in the androidApp module,
            // but you can add Android-only dependencies here if needed.
        }

        // --- Web Specific Dependencies (JS & Wasm) ---
        val jsMain by getting {
            // Web-specific dependencies or actual implementations go here.
            dependencies{
                implementation(compose.html.core)
                implementation(compose.components.resources)
                

            }

        }

        val wasmJsMain by getting {
            // Wasm-specific dependencies or actual implementations go here.


        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.example.process_scheduling.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    // CRITICAL: Enable Compose support for the Android target
    buildFeatures {
        compose = true
    }

    

}
