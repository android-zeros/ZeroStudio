plugins {
        id("com.android.library")
    id("kotlin-android")
    // alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.com.github.jk1.tcdeps)
    alias(libs.plugins.com.jaredsburrows.license)
      
}

android {
    namespace = "org.javacs.kt"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
      // buildTypes {
    // release {
      // isMinifyEnabled = false
      // proguardFiles("proguard-rules.pro")
    // }
  // }

}

kotlin {
    jvmToolchain(17)
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

dependencies {
    api(libs.org.jetbrains.kotlin.kotlin.scripting.jvm.host){
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.jline", module = "jline")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-jvm-host-unshaded")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-impl-embeddable")
        }
    api(libs.org.jetbrains.kotlin.stdlib){
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.jline", module = "jline")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-jvm-host-unshaded")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-impl-embeddable")
        }

    api(libs.org.jetbrains.kotlin.ktscompiler){
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.jline", module = "jline")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-jvm-host-unshaded")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-impl-embeddable")
        }

    api(libs.org.jetbrains.kotlin.sam.with.receiver.compiler.plugin){
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.jline", module = "jline")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-jvm-host-unshaded")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-impl-embeddable")
        }
    api(libs.org.jetbrains.kotlin.reflect){
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.jline", module = "jline")
        exclude(group = "net.java.dev.jna", module = "jna-platform")
        exclude(group = "net.java.dev.jna", module = "jna")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-jvm-host-unshaded")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-embeddable")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-scripting-compiler-impl-embeddable")
        }
       
    api(libs.hamcrest.all)
    api(libs.tests.junit)
    api(libs.com.google.guava.guava)
    api(libs.com.beust.jcommander)

    api(libs.org.openjdk.jmh.core)
    implementation(libs.org.jetbrains.exposed.core)
    implementation(libs.org.jetbrains.exposed.dao)
    implementation(libs.org.jetbrains.exposed.jdbc)
    implementation(libs.com.h2database.h2)
    implementation(libs.org.xerial.sqlite.jdbc)
    implementation(libs.kotlinx.coroutines)
    
    implementation(libs.common.lsp4j.jsonrpc)
    implementation(libs.common.org.eclipse.lsp4j)
    
    api(libs.org.openjdk.jmh.generator.annprocess)
    annotationProcessor(libs.org.openjdk.jmh.generator.annprocess)

    implementation(libs.common.editor)

    //本地资源
    implementation(projects.termux.emulator)
    implementation(projects.termux.shared)
    implementation(projects.logging.logsender)
    api(projects.core.lspApi)
    api(projects.core.lspModels)
    implementation(projects.event.eventbus)
    implementation(projects.event.eventbusAndroid)
    implementation(projects.event.eventbusEvents)
    api(projects.core.projects)
    api(projects.utilities.lookup)
    api(projects.utilities.preferences)
    api(projects.core.common)
    implementation(projects.core.actions)
    // implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar", "*.aar"))))
    implementation(project(":core:KotlinLsp:shared"))
    // implementation("zerostudio:kotlin-psitype:2.1.0")
    implementation(libs.com.github.fwcd.ktfmt)  //使用的是gradle/lib/zerostudio 下的依赖库
    implementation(files("libs/kotlin-compiler-2.2.0.jar"))
    // implementation(files("libs/kotlin-compiler-2.0.0-fork1.jar"))
    implementation(libs.xml.javax.stream)
    implementation(libs.org.jetbrains.fernflower) //本地仓库资源：gradle/libs
    
        // --- 测试依赖 ---
    testImplementation(libs.hamcrest.all)
    testImplementation(libs.tests.junit)
    testImplementation(libs.org.openjdk.jmh.core)
    testImplementation(projects.testing.commonTest)
    testImplementation(projects.testing.lspTest)
    androidTestImplementation(projects.testing.androidTest)
    androidTestImplementation(projects.utilities.shared)
    
}
