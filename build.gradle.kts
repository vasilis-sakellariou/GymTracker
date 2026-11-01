// Top-level build file: plugins shared by all modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Google services plugin (reads app/google-services.json for Firebase).
    id("com.google.gms.google-services") version "4.5.0" apply false
}
