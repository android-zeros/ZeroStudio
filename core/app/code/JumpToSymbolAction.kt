package com.itsaky.androidide.actions.cursor

import android.content.Context
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.itsaky.androidide.resources.R
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.EditorRelatedAction
import com.itsaky.androidide.actions.requireEditor
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.projects.ModuleProject
import com.itsaky.androidide.tasks.launchAsyncWithProgress
import com.itsaky.androidide.utils.DialogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import java.io.File

/**
 * An action to parse the current file and display a dialog with all symbols
 * (classes, functions, properties) to allow quick navigation.
 */
class GoToSymbolAction(private val context: Context, override val order: Int) : EditorRelatedAction() {
    init {
        id = "ide.editor.cursor.go_to_symbol"
        label = context.getString(R.string.action_editor_cursor_go_to_symbol)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_edit_line_next_position)
    }

    override suspend fun execAction(data: ActionData): Boolean {
        val editor = data.requireEditor()
        val activity = editor.context as? androidx.appcompat.app.AppCompatActivity ?: return false
        val file = data.get(File::class.java) ?: return false

        val module = IProjectManager.getInstance().requireWorkspace().findModuleForFile(file) as? ModuleProject

        activity.lifecycleScope.launchAsyncWithProgress(
            configureFlashbar = { builder, _ ->
                builder.message(R.string.action_go_to_symbol_parsing)
            }
        ) { _, _ ->
            val code = editor.text.toString()
            val classpaths = module?.getCompileClasspaths() ?: emptySet()
            
            val symbols = withContext(Dispatchers.IO) {
                parseSymbols(file.name, code, classpaths)
            }

            withContext(Dispatchers.Main) {
                if (symbols.isNotEmpty()) {
                    showSymbolDialog(activity, symbols)
                }
            }
        }
        return true
    }

    private fun parseSymbols(fileName: String, code: String, classpaths: Set<File>): List<SymbolInfo> {
        val symbols = mutableListOf<SymbolInfo>()
        val disposable = Disposer.newDisposable()
        try {
            val configuration = CompilerConfiguration()
            configuration.put(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
            )
            configuration.put(JVMConfigurationKeys.JDK_HOME, File(System.getProperty("java.home")!!))
            configuration.addJvmClasspathRoots(classpaths.toList())
            configuration.addKotlinSourceRoot("dummy.kt")
            
            val environment = KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
            val virtualFile = LightVirtualFile(fileName, KotlinFileType.INSTANCE, code)
            val ktFile = PsiManager.getInstance(environment.project).findFile(virtualFile) as? KtFile

            ktFile?.accept(object : KtTreeVisitorVoid() {
                override fun visitPackageDirective(directive: KtPackageDirective) {
                    val name = directive.qualifiedName
                    if (name.isNotEmpty()) {
                        val line = getLineNumber(code, directive.textOffset)
                        symbols.add(SymbolInfo(name, "Package", line, 0))
                    }
                    super.visitPackageDirective(directive)
                }

                override fun visitImportDirective(directive: KtImportDirective) {
                    val path = directive.importPath?.pathStr
                    if (path != null) {
                        val line = getLineNumber(code, directive.textOffset)
                        symbols.add(SymbolInfo(path, "Import", line, 0))
                    }
                    super.visitImportDirective(directive)
                }

                override fun visitClass(ktClass: KtClass) {
                    val line = getLineNumber(code, ktClass.textOffset)
                    val kind = when {
                        ktClass.isInterface() -> "Interface"
                        ktClass.isEnum() -> "Enum"
                        else -> "Class"
                    }
                    symbols.add(SymbolInfo(ktClass.name ?: "[anonymous]", kind, line, 0))
                    super.visitClass(ktClass)
                }

                override fun visitNamedFunction(function: KtNamedFunction) {
                    val line = getLineNumber(code, function.textOffset)
                    symbols.add(SymbolInfo(function.name ?: "[unnamed]", "Function", line, 0))
                    super.visitNamedFunction(function)
                }

                override fun visitProperty(property: KtProperty) {
                    val line = getLineNumber(code, property.textOffset)
                    val kind = if (property.isVar) "Variable" else "Value"
                    symbols.add(SymbolInfo(property.name ?: "[unnamed]", kind, line, 0))
                    super.visitProperty(property)
                }
            })
        } catch (e: Exception) {
            // Log error, might happen if compiler setup fails
            e.printStackTrace()
        } finally {
            Disposer.dispose(disposable)
        }
        return symbols.sortedBy { it.line }
    }

    private fun getLineNumber(text: String, offset: Int): Int {
        return text.substring(0, offset).count { it == '\n' }
    }
    
    private fun showSymbolDialog(context: Context, symbols: List<SymbolInfo>) {
        val editor = requireEditor()
        val displayItems = symbols.map { "${it.kind}: ${it.name}" }.toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle(R.string.action_editor_cursor_go_to_symbol)
            .setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, displayItems)) { dialog, which ->
                val symbol = symbols[which]
                editor.setSelection(symbol.line, symbol.column)
                editor.ensureSelectionVisible()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}