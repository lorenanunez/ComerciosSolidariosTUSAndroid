plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.lorenadev.app.comercios_solidatios_tus"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lorenadev.app.comercios_solidatios_tus"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation("org.projectlombok:lombok:1.18.48")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("org.apache.commons:commons-collections4:4.6.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("org.modelmapper:modelmapper:3.2.6")

    implementation("org.maplibre.gl:android-sdk:13.6.1")
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")

    annotationProcessor("org.projectlombok:lombok:1.18.48")
    annotationProcessor("androidx.room:room-compiler:2.8.5")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}