package com.itsaky.androidide.actions.code

import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.lang.java.JavaFileType
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.JavaToKotlinTranslator
import org.jetbrains.kotlin.j2k.OldJ2kConverter
import java.io.File

/**
 * Helper object to encapsulate the Java-to-Kotlin conversion logic using the Kotlin compiler's J2K converter.
 */
object J2kConverterHelper {

    fun convert(javaCode: String, classpaths: Set<File>): String? {
        val disposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration().apply {
                put(
                    CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                    PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
                )
                // Assuming a JDK is available on the system path for basic types
                System.getProperty("java.home")?.let { put(JVMConfigurationKeys.JDK_HOME, File(it)) }
                addJvmClasspathRoots(classpaths.toList())
            }

            val environment = KotlinCoreEnvironment.createForProduction(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
            val project = environment.project

            val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
            val javaFile = psiFileFactory.createFileFromText(
                "dummy.java",
                JavaFileType.INSTANCE,
                javaCode
            )

            val j2kConverter = OldJ2kConverter(
                project,
                ConverterSettings.defaultSettings,
                JavaToKotlinTranslator.INSTANCE
            )
            
            val results = j2kConverter.filesToKotlin(listOf(PsiUtilCore.getPsiFile(project, javaFile.virtualFile))).results
            return if (results.isNotEmpty()) results[0].text else null

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            Disposer.dispose(disposable)
        }
    }
}