import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "ru.zdevs"
version = "0.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        plugin("com.redhat.devtools.lsp4ij:0.16.1")
        bundledPlugin("org.jetbrains.plugins.textmate")
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1")
        }
    }
}


tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
