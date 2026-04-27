plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.nextalkapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nextalkapp"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    // Materials
    implementation("com.google.android.material:material:1.9.0")
    // Toasty
    implementation("com.github.GrenderG:Toasty:1.5.2")
    // MotionToast
    implementation("com.github.Spikeysanju:MotionToast:1.4")
    // JavaMail API dependencies for sending emails
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.9.0")) // Dùng bản ổn định 33.x
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage") // 🔥 Thêm dòng này để hết đỏ FirebaseStorage

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Nếu Duy dùng Java thì dùng annotationProcessor, nếu dùng Kotlin thì dùng kapt
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Các thư viện UI của Duy
    implementation("com.google.android.material:material:1.12.0") // Cập nhật bản mới hơn để hỗ trợ MaterialButton
    implementation("com.github.GrenderG:Toasty:1.5.2")
    implementation("com.github.Spikeysanju:MotionToast:1.4")
}