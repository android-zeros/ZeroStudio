package android.zero.studio.lsp.manager

import android.content.Context
import android.util.Log
import android.zero.studio.lsp.LspServerRegistry
import com.itsaky.androidide.editor.ui.IDEEditor
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.utils.FileUri
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Core manager for LSP integration in AndroidIDE.
 * Handles the lifecycle of LspProjects and LspEditors.
 * Thread-safe singleton.
 *
 * @author android_zero
 */
object LspManager {
    private const val TAG = "LspManager"

    // Map<ProjectPath, LspProject>
    private val projects = ConcurrentHashMap<String, LspProject>()
    
    // Map<EditorInstance, LspEditorWrapper>
    // We keep track of active editors to dispose them correctly.
    private val activeEditors = ConcurrentHashMap<CodeEditor, LspEditor>()

    /**
     * Initializes the LSP system for a specific project root.
     * Should be called when opening a project.
     */
    fun initProject(projectPath: String) {
        if (projects.containsKey(projectPath)) return

        Log.i(TAG, "Initializing LSP Project for: $projectPath")
        val lspProject = LspProject(projectPath)
        projects[projectPath] = lspProject
    }

    /**
     * Attaches an IDEEditor to an LSP session.
     * Automatically finds the correct server based on file extension.
     *
     * @param context Android Context
     * @param editor The UI editor component
     * @param file The file being edited
     * @param projectPath The root path of the current project
     * @param scope Coroutine scope for async operations
     */
    fun attachEditor(
        context: Context,
        editor: IDEEditor,
        file: File,
        projectPath: String,
        scope: CoroutineScope
    ) {
        val extension = file.extension
        val server = LspServerRegistry.findServerForExtension(extension)

        if (server == null) {
            Log.w(TAG, "No LSP server registered for extension: .$extension")
            return
        }

        if (!server.isInstalled(context)) {
            Log.w(TAG, "LSP server for .$extension is not installed.")
            // Optionally trigger installation UI here
            return
        }

        // Ensure project exists
        val lspProject = projects.computeIfAbsent(projectPath) { LspProject(it) }

        // Ensure server definition is added to the project
        // We use the extension as the key for the definition to allow multiple servers per project
        val definitionKey = extension // Or server.id
        
        // Note: sora-editor-lsp's addServerDefinition checks ext internally. 
        // We create a fresh definition wrapper that delegates to our BaseLspServer configuration.
        val definition = server.createDefinition(context)
        
        // We inject our own EventListener to bridge logs to Android Logcat
        val loggingDefinition = object : CustomLanguageServerDefinition(
             definition.ext, 
             { dir -> definition.createConnectionProvider(dir) } // Delegate factory
        ) {
             override fun getInitializationOptions(uri: java.net.URI?): Any? {
                 val fileUri = uri?.let { FileUri(it.path) }
                 return server.getInitializationOptions(fileUri)
             }

             override val eventListener: EventHandler.EventListener
                 get() = object : EventHandler.EventListener {
                     override fun initialize(server: LanguageServer?, result: InitializeResult) {
                         Log.i(TAG, "LSP Initialized for ${server.toString()}")
                     }
                     override fun onLogMessage(messageParams: MessageParams?) {
                         messageParams?.let { Log.i(TAG, "[LSP Log] ${it.message}") }
                     }
                     override fun onShowMessage(messageParams: MessageParams?) {
                         messageParams?.let { Log.i(TAG, "[LSP Message] ${it.type}: ${it.message}") }
                     }
                     override fun onHandlerException(exception: Exception) {
                         Log.e(TAG, "LSP Handler Exception", exception)
                     }
                 }
        }

        lspProject.addServerDefinition(loggingDefinition)

        // Create LspEditor wrapper
        scope.launch(Dispatchers.Main) {
            // Dispose previous wrapper if exists
            activeEditors.remove(editor)?.dispose()

            val lspEditor = lspProject.createEditor(file.absolutePath)
            lspEditor.editor = editor // Bind the IDEEditor instance itself, as it's a CodeEditor
            lspEditor.wrapperLanguage = editor.editorLanguage // Keep syntax highlighting

            activeEditors[editor] = lspEditor

            Log.i(TAG, "Connecting LSP Editor for ${file.name}...")
            
            withContext(Dispatchers.IO) {
                try {
                    // This blocks until initialized or timeout
                    val connected = lspEditor.connect()
                    if (connected) {
                        Log.i(TAG, "LSP Editor connected successfully.")
                    } else {
                        Log.e(TAG, "LSP Editor connection failed.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error connecting LSP", e)
                }
            }
        }
    }

    /**
     * Detaches LSP from an editor (e.g. when file is closed).
     */
    fun detachEditor(editor: CodeEditor) {
        activeEditors.remove(editor)?.let { lspEditor ->
            Log.d(TAG, "Disposing LSP Editor")
            lspEditor.dispose()
        }
    }

    /**
     * Shuts down a specific project and all its editors.
     */
    fun disposeProject(projectPath: String) {
        projects.remove(projectPath)?.let { project ->
            Log.i(TAG, "Disposing LSP Project: $projectPath")
            project.dispose()
        }
    }

    /**
     * Shuts down everything. Call on app exit.
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down LspManager")
        activeEditors.values.forEach { it.dispose() }
        activeEditors.clear()
        
        projects.values.forEach { it.dispose() }
        projects.clear()
    }
}