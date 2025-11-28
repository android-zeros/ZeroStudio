package com.itsaky.androidide.actions.code

import io.github.rosemoe.sora.text.CharPosition
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.io.File

data class OverridableMember(
    val signature: String,
    val sourceClass: String,
    val descriptor: DeclarationDescriptor,
    val containingClass: KtClassOrObject
)

object PsiSymbolResolver {

    fun findOverridableMembers(
        fileName: String,
        code: String,
        cursor: CharPosition,
        classpaths: Set<File>
    ): List<OverridableMember> {
        val disposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration().apply {
                put(
                    CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                    PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
                )
                // Assuming JDK is available in the environment
                System.getProperty("java.home")?.let { put(JVMConfigurationKeys.JDK_HOME, File(it)) }
                addJvmClasspathRoots(classpaths.toList())
                addKotlinSourceRoot("dummy.kt")
            }

            val environment = KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val virtualFile = LightVirtualFile(fileName, KotlinFileType.INSTANCE, code)
            val ktFile = PsiManager.getInstance(environment.project).findFile(virtualFile) as? KtFile ?: return emptyList()

            val elementAtCursor = ktFile.findElementAt(cursor.index) ?: return emptyList()
            val containingClass = elementAtCursor.containingClassOrObject ?: return emptyList()
            
            val classDescriptor = containingClass.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? ClassDescriptor ?: return emptyList()

            val allMembers = mutableMapOf<String, DeclarationDescriptor>()
            classDescriptor.unsubstitutedMemberScope.getContributedDescriptors().forEach { member ->
                if (member is CallableMemberDescriptor) {
                     allMembers[getSignature(member)] = member
                }
            }
            
            val overridableMembers = mutableListOf<OverridableMember>()
            classDescriptor.typeConstructor.supertypes.forEach { supertype ->
                val superDescriptor = supertype.constructor.declarationDescriptor as? ClassDescriptor ?: return@forEach
                superDescriptor.unsubstitutedMemberScope.getContributedDescriptors().forEach { member ->
                     if (member is CallableMemberDescriptor && (member.modality == Modality.OPEN || member.modality == Modality.ABSTRACT)) {
                        val signature = getSignature(member)
                        if (!allMembers.containsKey(signature) || (allMembers[signature] as? CallableMemberDescriptor)?.modality == Modality.ABSTRACT) {
                             overridableMembers.add(OverridableMember(
                                signature,
                                superDescriptor.fqNameSafe.asString(),
                                member,
                                containingClass
                            ))
                        }
                    }
                }
            }
            // Remove duplicates
            return overridableMembers.distinctBy { it.signature }.sortedBy { it.signature }

        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun getSignature(descriptor: DeclarationDescriptor): String {
        return when (descriptor) {
            is FunctionDescriptor -> {
                val params = descriptor.valueParameters.joinToString(", ") { "${it.name}: ${it.type}" }
                "${descriptor.name}($params): ${descriptor.returnType}"
            }
            is PropertyDescriptor -> {
                val keyword = if (descriptor.isVar) "var" else "val"
                "$keyword ${descriptor.name}: ${descriptor.type}"
            }
            else -> descriptor.name.asString()
        }
    }
}

object CodeGenerator {
    fun generateOverrideMethods(members: List<OverridableMember>): String {
        val indent = "    "
        return members.joinToString("\n\n") { member ->
            when (val descriptor = member.descriptor) {
                is FunctionDescriptor -> {
                    val params = descriptor.valueParameters.joinToString(", ") { "${it.name}: ${it.type}" }
                    val returnType = if (descriptor.returnType.toString() != "Unit") ": ${descriptor.returnType}" else ""
                    val body = if (descriptor.modality == Modality.ABSTRACT) {
                        "TODO(\"Not yet implemented\")"
                    } else {
                        "super.${descriptor.name}(${descriptor.valueParameters.joinToString(", ") { it.name }})"
                    }
                    "$indent override fun ${descriptor.name}($params)$returnType {\n$indent$indent$body\n$indent}"
                }
                is PropertyDescriptor -> {
                    val keyword = if (descriptor.isVar) "var" else "val"
                    val body = if (descriptor.modality == Modality.ABSTRACT) {
                        "\n$indent$indent get() = TODO(\"Not yet implemented\")" +
                        if (descriptor.isVar) "\n$indent$indent set(value) {}" else ""
                    } else {
                        "\n$indent$indent get() = super.${descriptor.name}" +
                        if (descriptor.isVar) "\n$indent$indent set(value) { super.${descriptor.name} = value }" else ""
                    }
                    "$indent override $keyword ${descriptor.name}: ${descriptor.type}$body"
                }
                else -> ""
            }
        }
    }
}