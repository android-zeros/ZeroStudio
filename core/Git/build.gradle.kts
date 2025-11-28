import com.itsaky.androidide.build.config.BuildConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.androidx.room)
  alias(libs.plugins.com.google.devtools.ksp)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
  alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    id("kotlin-parcelize")

}
val dbSchemaLocation="$projectDir/schemas"
room {
    schemaDirectory(dbSchemaLocation)
}
android {

    val packageName = "com.catpuppyapp.puppygit.play.pro"

    namespace = packageName
    compileSdk = 36
    ndkVersion = "27.1.12297006"
    
    defaultConfig {
        minSdk = 26

        buildConfigField("String", "FILE_PROVIDIER_AUTHORITY", """"$packageName.file_provider"""")
        resValue("string", "file_provider_authority", "$packageName.file_provider")


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
        // "x86",
            abiFilters += listOf("arm64-v8a","x86_64","armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                arguments+= listOf("-DANDROID_STL=none")
            }
        }

    }
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.31.1"
        }
    }

    buildTypes {
    
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "gson.pro"
            )
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/io.netty.versions.properties")

        }
    }


}

dependencies {

    // Core & AppCompat
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    coreLibraryDesugaring(libs.androidx.libDesugaring)
    
    // Splash Screen
    implementation(libs.androidx.splashscreen)

    // Compose (BOM 平台管理所有 Compose 库的版本)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Lifecycle
    implementation(libs.bundles.androidx.lifecycle)

    // Room
    implementation(libs.bundles.androidx.room)
    ksp(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Markwon
    implementation(libs.common.markwon.jeziellago)
    implementation(libs.bundles.io.markwon)

    // Coil
    implementation(libs.bundles.io.coil)

    // Ktor (HTTP Server)
    implementation(libs.bundles.io.ktor)

    // 其他工具库
    implementation(libs.github.juniversalchardet)
    implementation(libs.common.org.eclipse.jdt.annotation)
    implementation(libs.common.org.jruby.joni)
    implementation(libs.google.gson)
    implementation(libs.common.snakeyaml.engine)
    implementation(libs.common.bcrypt)
    implementation(libs.androidx.documentfile)
    implementation(libs.google.findbugs.jsr305)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(projects.core.resources)
    
    // Testing
    testImplementation(libs.tests.junit)
    androidTestImplementation(libs.tests.androidx.junit)
    androidTestImplementation(libs.tests.androidx.espresso.core)
    
    // Android Test for Compose
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.compose.test)

    // Debug for Compose
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
