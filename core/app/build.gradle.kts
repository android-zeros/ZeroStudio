@file:Suppress("UnstableApiUsage")

import com.itsaky.androidide.build.config.BuildConfig
import com.itsaky.androidide.desugaring.utils.JavaIOReplacements.applyJavaIOReplacements
import com.itsaky.androidide.plugins.AndroidIDEAssetsPlugin
import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
  id("com.itsaky.androidide.core-app")
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
  id("kotlin-parcelize")
  id("androidx.navigation.safeargs.kotlin")
  id("com.itsaky.androidide.desugaring")
}

apply {
  plugin(AndroidIDEAssetsPlugin::class.java)
}

buildscript {
  dependencies {
    classpath(libs.logging.logback.core)
    classpath(libs.composite.desugaringCore)
  }
}

android {
  namespace = BuildConfig.packageName

  defaultConfig {
    applicationId = BuildConfig.packageName
    vectorDrawables.useSupportLibrary = true
  }

  androidResources {
    generateLocaleConfig = true
  }


    signingConfigs {
        create("all") {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("signing.properties")
            enableV1Signing = true // 启用 V1 签名
            enableV2Signing = true // 启用 V2 签名 (推荐，Android 7.0+)
            enableV3Signing = true // 启用 V3 签名 (推荐，Android 9+)
            enableV4Signing = true
            
            if (localPropertiesFile.exists()) {
                localProperties.load(FileInputStream(localPropertiesFile))
                
                val storeFilePath = localProperties.getProperty("storeFile")
                val storePasswordValue = localProperties.getProperty("storePassword")
                val keyAliasValue = localProperties.getProperty("keyAlias")
                val keyPasswordValue = localProperties.getProperty("keyPassword")
                
                if (storeFilePath != null && storePasswordValue != null && 
                    keyAliasValue != null && keyPasswordValue != null) {
                    storeFile = file(storeFilePath)
                    storePassword = storePasswordValue
                    keyAlias = keyAliasValue
                    keyPassword = keyPasswordValue
                }
            }
        }
    }
      buildTypes {
      all{
      signingConfig = signingConfigs.getByName("all")

      }

    debug{
    isShrinkResources = false
    }
    release {
    isMinifyEnabled = false
      // isShrinkResources = true

    }
  }
    
    packaging.resources.excludes += listOf(
    "**/*.kotlin_builtins", 
    "kotlin/kotlin.kotlin_builtins", 
    "THIRD-PARTY", 
    "META-INF/DEPENDENCIES", 
    "META-INF/NOTICE.md", 
    "com/android/builder/model/version.properties", 
    "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    
    
  lint {
    abortOnError = false
    disable.addAll(arrayOf("VectorPath", "NestedWeights", "ContentDescription", "SmallSp"))
  }
}

kapt {
  arguments {
    arg("eventBusIndex", "${BuildConfig.packageName}.events.AppEventsIndex")
  }
}

desugaring {
  replacements {
    includePackage(
      "org.eclipse.jgit",
    )

    applyJavaIOReplacements()
  }
}

configurations.all {
    resolutionStrategy {
        force(libs.hamcrest.all)
        force(libs.tests.junit)
        force(libs.common.lsp4j.jsonrpc)
        force(libs.common.org.eclipse.lsp4j)

        force(libs.org.jetbrains.kotlin.stdlib)
        force(libs.org.jetbrains.kotlin.compiler)
        force(libs.org.jetbrains.kotlin.kotlin.scripting.jvm.host)
        force(libs.org.jetbrains.kotlin.ktscompiler)
        force(libs.org.jetbrains.kotlin.sam.with.receiver.compiler.plugin)
        force(libs.org.jetbrains.kotlin.reflect)
        force(libs.org.jetbrains.kotlin.jvm)
        
        force(libs.google.protobuf)
    }
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "com.android.tools.build", module = "builder-model")
}

dependencies {
  
  
  // Lottie Animation SDK
  implementation(libs.common.com.airbnb.android.lottie)

  // Annotation processors
  kapt(libs.common.glide.ap)
  kapt(libs.google.auto.service)
  kapt(projects.annotation.processors)
  
  implementation(libs.com.github.fwcd.ktfmt)
  
  implementation(libs.common.editor)
  implementation(libs.common.utilcode)
  implementation(libs.common.glide)
  implementation(libs.common.jsoup)
  implementation(libs.common.kotlin.coroutines.android)
  implementation(libs.common.retrofit)
  implementation(libs.common.retrofit.gson)
  implementation(libs.common.charts)
  implementation(libs.common.hiddenApiBypass)
  implementation(libs.aapt2.common)
  implementation(libs.common.org.apache.commons.compress)
  implementation(libs.common.org.tukaani.tarxzip)
  
  implementation(libs.google.auto.service.annotations)
  implementation(libs.google.gson)
  implementation(libs.google.guava)

  // Git
  implementation(libs.git.jgit)

  // AndroidX
  implementation(libs.androidx.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.cardview)
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.coordinatorlayout)
  implementation(libs.androidx.drawer)
  implementation(libs.androidx.grid)
  implementation(libs.androidx.nav.fragment)
  implementation(libs.androidx.nav.ui)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.transition)
  implementation(libs.androidx.vectors)
  implementation(libs.androidx.animated.vectors)
  implementation(libs.androidx.work)
  implementation(libs.androidx.work.ktx)
  implementation(libs.google.material)
  implementation(libs.google.flexbox)
  
  //UI/UX
  implementation(libs.bundles.compose) //androidx compose
  
  // Kotlin
  implementation(libs.androidx.core.ktx)
  implementation(libs.common.kotlin)

  // Dependencies in composite build
  implementation(libs.composite.appintro)
  implementation(libs.composite.desugaringCore)
  implementation(libs.composite.javapoet)

  // java格式化
  implementation(libs.composite.googleJavaFormat){
    exclude(group = "com.google.guava", module = "guava")
  }
   
   implementation(libs.common.org.eclipse.lsp4j)
   implementation(libs.common.lsp4j.jsonrpc)
    
  // Local projects here
  implementation(projects.core.actions)
  implementation(projects.core.common)
  implementation(projects.core.indexingApi)
  implementation(projects.core.indexingCore)
  implementation(projects.core.lspApi)
  implementation(projects.core.projects)
  implementation(projects.core.resources)
  implementation(projects.editor.impl)
  implementation(projects.editor.lexers)
  implementation(projects.editor.editorLsp)
  implementation(projects.java.javacServices)
  implementation(projects.java.lsp)
  implementation(projects.logging.idestats)
  implementation(projects.logging.logsender)
  implementation(projects.termux.application)
  implementation(projects.termux.view)
  implementation(projects.termux.emulator)
  implementation(projects.termux.shared)
  implementation(projects.xml.aaptcompiler)
  implementation(projects.xml.lsp)
  implementation(projects.xml.utils)
  implementation(projects.tooling.api)
  implementation(projects.tooling.pluginConfig)
    // This is to build the tooling-api-impl project before the app is built
    // So we always copy the latest JAR file to assets
    compileOnly(projects.tooling.impl)
    implementation(projects.utilities.buildInfo)
    implementation(projects.utilities.lookup)
    implementation(projects.utilities.flashbar)
    implementation(projects.utilities.preferences)
    implementation(projects.utilities.templatesApi)
    implementation(projects.utilities.templatesImpl)
    implementation(projects.utilities.treeview)
    implementation(projects.utilities.uidesigner)
    implementation(projects.utilities.xmlInflater)
    implementation(projects.event.eventbusAndroid)
    implementation(projects.event.eventbusEvents)
    implementation(projects.event.eventbus)
    implementation(projects.event.eventbusAndroid)
    implementation(projects.event.eventbusEvents)
   // implementation(project(":chatai:home"))
    // implementation(project(":chatai:ai"))
    // implementation(project(":chatai:highlight"))
    // implementation(project(":chatai:search"))
    // implementation(project(":chatai:rag"))
    implementation(project(":modules:MTDataFilesProvider"))
   // implementation(project(":core:KotlinLsp:server"))
   // implementation(project(":core:KotlinLsp:shared"))


  testImplementation("org.conscrypt:conscrypt-openjdk:2.5.2")
  testImplementation(projects.testing.unitTest)
  androidTestImplementation(projects.testing.androidTest)
  debugImplementation(libs.common.leakcanary)
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

}
