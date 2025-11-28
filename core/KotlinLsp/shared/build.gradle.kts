plugins {
    id("com.android.library")
    id("kotlin-android")

}


android {
namespace = "org.javacs.kt.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        
    }

    kotlinOptions {
        jvmTarget = "11" 
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
configurations.all {
    resolutionStrategy {
        force(libs.org.jetbrains.kotlin.stdlib)
        force(libs.hamcrest.all)
        force(libs.tests.junit)
        force(libs.common.lsp4j.jsonrpc)
        force(libs.common.org.eclipse.lsp4j)
        force(libs.org.jetbrains.kotlin.compiler)
        force(libs.org.jetbrains.kotlin.kotlin.scripting.jvm.host)
        force(libs.org.jetbrains.kotlin.ktscompiler)
        force(libs.org.jetbrains.kotlin.kts.jvm.host.unshaded)
        force(libs.org.jetbrains.kotlin.sam.with.receiver.compiler.plugin)
        force(libs.org.jetbrains.kotlin.reflect)
        force(libs.org.jetbrains.exposed.core)
        force(libs.org.jetbrains.exposed.dao)
        force(libs.org.jetbrains.exposed.jdbc)
        force(libs.com.google.guava.guava)
        
    }
          exclude(group = "org.hamcrest", module = "hamcrest-core")
}

}
dependencies {
    implementation(libs.org.xerial.sqlite.jdbc)
    implementation(libs.org.jetbrains.exposed.core)
    implementation(libs.org.jetbrains.exposed.dao)
    api(libs.org.jetbrains.exposed.jdbc)

    // --- 测试依赖 ---
    testImplementation(libs.tests.junit)
    testImplementation(projects.testing.commonTest)
    testImplementation(projects.testing.lspTest)
    androidTestImplementation(projects.testing.androidTest)
    androidTestImplementation(projects.utilities.shared)
}

kotlin {
    jvmToolchain(11)
}

