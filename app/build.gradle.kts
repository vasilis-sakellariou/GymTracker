plugins {
    alias(libs.plugins.android.application)
    // Reads google-services.json and connects the app to the Firebase project.
    id("com.google.gms.google-services")
}

android {
    namespace = "com.vasilis.gymtracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.vasilis.gymtracker"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Firebase: the BoM picks versions that work together,
    // so the lines below don't specify a version.
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")       // sign in / sign up / password reset
    implementation("com.google.firebase:firebase-firestore")  // database

    // Line charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Swipeable tabs and lists
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
