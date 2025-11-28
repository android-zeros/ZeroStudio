import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
}

android {
    compileSdk = 36
    namespace = "com.itsvks.layouteditor"

    defaultConfig {

        minSdk = 26
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}


dependencies {

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.palette.ktx)

    implementation(libs.androidx.google.fonts)
    implementation(libs.google.material)
    implementation(libs.google.gson)

    implementation(libs.common.utilcode)
    implementation(libs.common.glide)
    implementation(libs.common.zoomage)
    implementation(libs.common.colorpickerview)
    implementation(libs.commons.text)
    implementation(libs.common.io)
    implementation(libs.common.editor)
    implementation(libs.common.sora.language.textmate)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    coreLibraryDesugaring(libs.androidx.libDesugaring)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    //本地资源
    implementation(projects.xml.vectormaster)
    implementation(projects.core.resources)
}



kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
