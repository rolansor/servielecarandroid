plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.serviaux"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.serviaux"
        minSdk = 26
        // Alineado con compileSdk: quedarse atrás renuncia a los endurecimientos de la
        // plataforma. Android 16 fuerza edge-to-edge, que la app ya usa.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 ofusca y elimina el código no usado. Reduce mucho el APK (se empaqueta
            // material-icons-extended completo) y dificulta la ingeniería inversa.
            // Las reglas de lo que no se puede tocar están en proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // PENDIENTE: definir signingConfig con el keystore del taller. Sin esto el APK de
            // release sale sin firmar y Android Studio lo firma con la clave de depuración,
            // que no sirve para distribuir ni permite actualizar una instalación existente.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// El esquema de Room se versiona en app/schemas: es lo que permite escribir migraciones
// correctas (y verificarlas) en lugar de recurrir a un borrado destructivo de la base.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Biometric
    implementation(libs.androidx.biometric)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Dropbox
    implementation(libs.dropbox.core.sdk)
    implementation(libs.dropbox.android.sdk)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
