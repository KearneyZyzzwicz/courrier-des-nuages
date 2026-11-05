import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

// Numéro de version fourni par GitHub à chaque fabrication (1 en local).
val numeroVersion = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val cheminCle = project.findProperty("keystorePath") as String?
val motDePasseCle: String? = System.getenv("SIGNING_KEY_PASSWORD")

android {
    namespace = "fr.courrierdesnuages.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.courrierdesnuages.app"
        minSdk = 26
        targetSdk = 35
        versionCode = numeroVersion
        versionName = "1.$numeroVersion"
    }

    signingConfigs {
        if (cheminCle != null && motDePasseCle != null) {
            create("release") {
                storeFile = file(cheminCle)
                storePassword = motDePasseCle
                keyAlias = "courrier"
                keyPassword = motDePasseCle
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")

    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}
