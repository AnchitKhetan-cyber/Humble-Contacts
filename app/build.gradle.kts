plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
}

android {

    namespace = "com.humblesolutions.humblecontacts"
    compileSdk = 36

    defaultConfig {

        applicationId =
            "com.humblesolutions.humblecontacts"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }


    kotlinOptions {

        jvmTarget = "17"

    }


    buildFeatures {

        compose = true

    }

}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.androidx.material3)

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.7"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7"
    )

    // Firebase
    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.12.0"
        )
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1"
    )

    implementation(
        "androidx.navigation:navigation-compose:2.8.5"
    )

    implementation(
        "androidx.compose.foundation:foundation"
    )

    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.animation)

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // Google Identity — provides GetGoogleIdOption
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")


    implementation(platform("androidx.compose:compose-bom:<latest>"))
    implementation("androidx.compose.animation:animation")


}
