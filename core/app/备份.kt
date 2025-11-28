
/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.activities.editor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams
import androidx.annotation.DrawableRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.collection.MutableIntObjectMap
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import com.blankj.utilcode.util.ImageUtils
import com.itsaky.androidide.R.string
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.ActionItem.Location.EDITOR_TOOLBAR
import com.itsaky.androidide.actions.ActionsRegistry.Companion.getInstance
import com.itsaky.androidide.actions.FillMenuParams
import com.itsaky.androidide.editor.language.treesitter.JavaLanguage
import com.itsaky.androidide.editor.language.treesitter.JsonLanguage
import com.itsaky.androidide.editor.language.treesitter.KotlinLanguage
import com.itsaky.androidide.editor.language.treesitter.LogLanguage
import com.itsaky.androidide.editor.language.treesitter.TSLanguageRegistry
import com.itsaky.androidide.editor.language.treesitter.XMLLanguage
import com.itsaky.androidide.editor.schemes.IDEColorSchemeProvider
import com.itsaky.androidide.editor.ui.IDEEditor
import com.itsaky.androidide.eventbus.events.editor.DocumentChangeEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentSaveEvent
import com.itsaky.androidide.eventbus.events.file.FileRenameEvent
import com.itsaky.androidide.eventbus.events.preferences.PreferenceChangeEvent
import com.itsaky.androidide.interfaces.IEditorHandler
import com.itsaky.androidide.models.FileExtension
import com.itsaky.androidide.models.OpenedFile
import com.itsaky.androidide.models.OpenedFilesCache
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.models.SaveResult
import com.itsaky.androidide.preferences.internal.EditorPreferences
import com.itsaky.androidide.projects.internal.ProjectManagerImpl
import com.itsaky.androidide.tasks.executeAsync
import com.itsaky.androidide.ui.CodeEditorView
import com.itsaky.androidide.utils.DialogUtils.newYesNoDialog
import com.itsaky.androidide.utils.IntentUtils.openImage
import com.itsaky.androidide.utils.UniqueNameBuilder
import com.itsaky.androidide.utils.flashSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.set

/**
 * Base class for EditorActivity. Handles logic for working with file editors.
 *
 * @author Akash Yadav
 * @author android_zero
 */
open class EditorHandlerActivity : ProjectHandlerActivity(), IEditorHandler {

  protected val isOpenedFilesSaved = AtomicBoolean(false)

  override fun doOpenFile(file: File, selection: Range?) {
    openFileAndSelect(file, selection)
  }

  override fun doCloseAll(runAfter: () -> Unit) {
    closeAll(runAfter)
  }

  override fun provideCurrentEditor(): CodeEditorView? {
    return getCurrentEditor()
  }

  override fun provideEditorAt(index: Int): CodeEditorView? {
    return getEditorAtIndex(index)
  }

  override fun preDestroy() {
    super.preDestroy()
    TSLanguageRegistry.instance.destroy()
    editorViewModel.removeAllFiles()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    mBuildEventListener.setActivity(this)
    super.onCreate(savedInstanceState)

    editorViewModel._displayedFile.observe(
      this) { this.content.editorContainer.displayedChild = it }
    editorViewModel._startDrawerOpened.observe(this) { opened ->
      this.binding.editorDrawerLayout.apply {
        if (opened) openDrawer(GravityCompat.START) else closeDrawer(GravityCompat.START)
      }
    }

    editorViewModel._filesModified.observe(this) { invalidateOptionsMenu() }
    editorViewModel._filesSaving.observe(this) { invalidateOptionsMenu() }

    editorViewModel.observeFiles(this) {
      // rewrite the cached files index if there are any opened files
      val currentFile =
        getCurrentEditor()?.editor?.file?.absolutePath
          ?: run {
            editorViewModel.writeOpenedFiles(null)
            editorViewModel.openedFilesCache = null
            return@observeFiles
          }
      getOpenedFiles().also {
        val cache = OpenedFilesCache(currentFile, it)
        editorViewModel.writeOpenedFiles(cache)
        editorViewModel.openedFilesCache = cache
      }
    }

    executeAsync {
      TSLanguageRegistry.instance.register(JavaLanguage.TS_TYPE, JavaLanguage.FACTORY)
      TSLanguageRegistry.instance.register(KotlinLanguage.TS_TYPE_KT, KotlinLanguage.FACTORY)
      TSLanguageRegistry.instance.register(KotlinLanguage.TS_TYPE_KTS, KotlinLanguage.FACTORY)
      TSLanguageRegistry.instance.register(LogLanguage.TS_TYPE, LogLanguage.FACTORY)
      TSLanguageRegistry.instance.register(JsonLanguage.TS_TYPE, JsonLanguage.FACTORY)
      TSLanguageRegistry.instance.register(XMLLanguage.TS_TYPE, XMLLanguage.FACTORY)
      IDEColorSchemeProvider.initIfNeeded()
    }
  }

  override fun onPause() {
    super.onPause()

    // if the user manually closes the project, this will be true
    // in this case, don't overwrite the already saved cache
    if (!isOpenedFilesSaved.get()) {
      saveOpenedFiles()
    }
  }

  override fun onResume() {
    super.onResume()
    isOpenedFilesSaved.set(false)
  }

  override fun saveOpenedFiles() {
    writeOpenedFilesCache(getOpenedFiles(), getCurrentEditor()?.editor?.file)
  }

  private fun writeOpenedFilesCache(openedFiles: List<OpenedFile>, selectedFile: File?) {
    if (selectedFile == null || openedFiles.isEmpty()) {
      editorViewModel.writeOpenedFiles(null)
      editorViewModel.openedFilesCache = null
      log.debug("[onPause] No opened files. Opened files cache reset to null.")
      isOpenedFilesSaved.set(true)
      return
    }

    val cache = OpenedFilesCache(selectedFile = selectedFile.absolutePath, allFiles = openedFiles)

    editorViewModel.writeOpenedFiles(cache)
    editorViewModel.openedFilesCache = if (!isDestroying) cache else null
    log.debug("[onPause] Opened files cache reset to {}", editorViewModel.openedFilesCache)
    isOpenedFilesSaved.set(true)
  }

  override fun onStart() {
    super.onStart()

    try {
      editorViewModel.getOrReadOpenedFilesCache(this::onReadOpenedFilesCache)
      editorViewModel.openedFilesCache = null
    } catch (err: Throwable) {
      log.error("Failed to reopen recently opened files", err)
    }
  }

  private fun onReadOpenedFilesCache(cache: OpenedFilesCache?) {
    cache ?: return
    cache.allFiles.forEach { file ->
      openFile(File(file.filePath), file.selection)
    }
    openFile(File(cache.selectedFile))
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    prepareOptionsMenu(menu)
    return true
  }

  @SuppressLint("RestrictedApi")
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    if (menu is MenuBuilder) {
      menu.setOptionalIconsVisible(true)
    }

    val data = createToolbarActionData()
    getInstance().fillMenu(FillMenuParams(data, EDITOR_TOOLBAR, menu))
    return true
  }

  open fun prepareOptionsMenu(menu: Menu) {
    val data = createToolbarActionData()
    val actions = getInstance().getActions(EDITOR_TOOLBAR)
    actions.forEach { (_, action) ->
      menu.findItem(action.itemId)?.let { item ->
        action.prepare(data)

        item.isVisible = action.visible
        item.isEnabled = action.enabled
        item.title = action.label

        item.icon = action.icon?.apply {
          colorFilter = action.createColorFilter(data)
          alpha = if (action.enabled) 255 else 76
        }

        var showAsAction = action.getShowAsActionFlags(data)
        if (showAsAction == -1) {
          showAsAction = if (action.icon != null) {
            MenuItem.SHOW_AS_ACTION_IF_ROOM
          } else {
            MenuItem.SHOW_AS_ACTION_NEVER
          }
        }

        if (!action.enabled) {
          showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
        }

        item.setShowAsAction(showAsAction)

        action.createActionView(data)?.let { item.actionView = it }
      }
    }
    content.editorToolbar.updateMenuDisplay()
  }

  private fun createToolbarActionData(): ActionData {
    val data = ActionData()
    val currentEditor = getCurrentEditor()

    data.put(Context::class.java, this)
    data.put(CodeEditorView::class.java, currentEditor)

    if (currentEditor != null) {
      data.put(IDEEditor::class.java, currentEditor.editor)
      data.put(File::class.java, currentEditor.file)
    }
    return data
  }

  override fun getCurrentEditor(): CodeEditorView? {
    return if (editorViewModel.getCurrentFileIndex() != -1) {
      getEditorAtIndex(editorViewModel.getCurrentFileIndex())
    } else null
  }

  override fun getEditorAtIndex(index: Int): CodeEditorView? {
    return _binding?.content?.editorContainer?.getChildAt(index) as CodeEditorView?
  }

  override fun openFileAndSelect(file: File, selection: Range?) {
    openFile(file, selection)

    getEditorForFile(file)?.editor?.also { editor ->
      editor.postInLifecycle {
        if (selection == null) {
          editor.setSelection(0, 0)
          return@postInLifecycle
        }

        editor.validateRange(selection)
        editor.setSelection(selection)
      }
    }
  }

  override fun openFile(file: File, selection: Range?): CodeEditorView? {
    val range = selection ?: Range.NONE
    if (ImageUtils.isImage(file)) {
      openImage(this, file)
      return null
    }

    val index = openFileAndGetIndex(file, range)
    val tab = content.tabs.getTabAt(index)
    if (tab != null && index >= 0 && !tab.isSelected) {
      tab.select()
    }

    editorViewModel.startDrawerOpened = false
    editorViewModel.displayedFileIndex = index

    return try {
      getEditorAtIndex(index)
    } catch (th: Throwable) {
      log.error("Unable to get editor fragment at opened file index {}", index, th)
      null
    }
  }

  override fun openFileAndGetIndex(file: File, selection: Range?): Int {
    val openedFileIndex = findIndexOfEditorByFile(file)
    if (openedFileIndex != -1) {
      return openedFileIndex
    }

    if (!file.exists()) {
      return -1
    }

    val position = editorViewModel.getOpenedFileCount()

    log.info("Opening file at index {} file:{}", position, file)

    val editor = CodeEditorView(this, file, selection!!)
    editor.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    content.editorContainer.addView(editor)
    content.tabs.addTab(content.tabs.newTab())

    editorViewModel.addFile(file)
    editorViewModel.setCurrentFile(position, file)

    updateTabs()

    return position
  }

  override fun getEditorForFile(file: File): CodeEditorView? {
    for (i in 0 until editorViewModel.getOpenedFileCount()) {
      val editor = content.editorContainer.getChildAt(i) as? CodeEditorView
      if (file == editor?.file) return editor
    }
    return null
  }

  override fun findIndexOfEditorByFile(file: File?): Int {
    if (file == null) {
      log.error("Cannot find index of a null file.")
      return -1
    }

    for (i in 0 until editorViewModel.getOpenedFileCount()) {
      val opened: File = editorViewModel.getOpenedFile(i)
      if (opened == file) {
        return i
      }
    }

    return -1
  }

  override fun saveAllAsync(
    notify: Boolean,
    requestSync: Boolean,
    processResources: Boolean,
    progressConsumer: ((Int, Int) -> Unit)?,
    runAfter: (() -> Unit)?
  ) {
    editorActivityScope.launch {
      saveAll(notify, requestSync, processResources, progressConsumer)
      runAfter?.invoke()
    }
  }

  override suspend fun saveAll(
    notify: Boolean,
    requestSync: Boolean,
    processResources: Boolean,
    progressConsumer: ((Int, Int) -> Unit)?
  ): Boolean {
    val result = saveAllResult(progressConsumer)

    // don't bother to switch the context if we don't need to
    if (notify || (result.gradleSaved && requestSync)) {
      withContext(Dispatchers.Main) {
        if (notify) {
          flashSuccess(string.all_saved)
        }

        if (result.gradleSaved && requestSync) {
          editorViewModel.isSyncNeeded = true
        }
      }
    }

    if (processResources) {
      ProjectManagerImpl.getInstance().generateSources()
    }

    return result.gradleSaved
  }

  override suspend fun saveAllResult(progressConsumer: ((Int, Int) -> Unit)?): SaveResult {
    return performFileSave {
      val result = SaveResult()
      for (i in 0 until editorViewModel.getOpenedFileCount()) {
        saveResultInternal(i, result)
        progressConsumer?.invoke(i + 1, editorViewModel.getOpenedFileCount())
      }

      return@performFileSave result
    }
  }

  override suspend fun saveResult(index: Int, result: SaveResult) {
    performFileSave {
      saveResultInternal(index, result)
    }
  }

  private suspend fun saveResultInternal(
    index: Int,
    result: SaveResult
  ) : Boolean {
    if (index < 0 || index >= editorViewModel.getOpenedFileCount()) {
      return false
    }

    val frag = getEditorAtIndex(index) ?: return false
    val fileName = frag.file?.name ?: return false

    run {
      // Must be called before frag.save()
      // Otherwise, it'll always return false
      val modified = frag.isModified
      if (!frag.save()) {
        return false
      }

      val isGradle = fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts")
      val isXml: Boolean = fileName.endsWith(".xml")
      if (!result.gradleSaved) {
        result.gradleSaved = modified && isGradle
      }

      if (!result.xmlSaved) {
        result.xmlSaved = modified && isXml
      }
    }

    val hasUnsaved = hasUnsavedFiles()

    withContext(Dispatchers.Main) {
      updateModificationState()
      // editorViewModel.areFilesModified = hasUnsaved

      // set tab as unmodified
      val tab = content.tabs.getTabAt(index) ?: return@withContext
      if (tab.text!!.startsWith('*')) {
        tab.text = tab.text!!.substring(startIndex = 1)
      }
    }

    return true
  }

  private fun hasUnsavedFiles() = editorViewModel.getOpenedFiles().any { file ->
    getEditorForFile(file)?.isModified == true
  }
  
  /**
   * Central method to check if any open editor has modifications and update the ViewModel.
   */
  private fun updateModificationState() {
      editorViewModel.areFilesModified = hasUnsavedFiles()
  }

  private suspend inline fun <T : Any?> performFileSave(crossinline action: suspend () -> T) : T {
    setFilesSaving(true)
    try {
      return action()
    } finally {
      setFilesSaving(false)
    }
  }

  private suspend fun setFilesSaving(saving: Boolean) {
    withContext(Dispatchers.Main.immediate) {
      editorViewModel.areFilesSaving = saving
    }
  }

  override fun areFilesModified(): Boolean {
    return editorViewModel.areFilesModified
  }

  override fun areFilesSaving(): Boolean {
    return editorViewModel.areFilesSaving
  }

  override fun closeFile(index: Int, runAfter: () -> Unit) {
    if (index < 0 || index >= editorViewModel.getOpenedFileCount()) {
      log.error("Invalid file index. Cannot close.")
      return
    }

    val opened = editorViewModel.getOpenedFile(index)
    log.info("Closing file: {}", opened)

    val editor = getEditorAtIndex(index)
    if (editor?.isModified == true) {
      log.info("File has been modified: {}", opened)
      notifyFilesUnsaved(listOf(editor)) {
        closeFile(index, runAfter)
      }
      return
    }

    editor?.close() ?: run {
      log.error("Cannot save file before close. Editor instance is null")
    }

    editorViewModel.removeFile(index)
    content.apply {
      tabs.removeTabAt(index)
      editorContainer.removeViewAt(index)
    }

    editorViewModel.areFilesModified = hasUnsavedFiles()

    updateTabs()
    runAfter()
  }

  override fun closeOthers() {
    if (editorViewModel.getOpenedFileCount() == 0) {
      return
    }

    val unsavedFiles =
      editorViewModel.getOpenedFiles().map(::getEditorForFile)
        .filter { it != null && it.isModified }

    if (unsavedFiles.isNotEmpty()) {
      notifyFilesUnsaved(unsavedFiles) { closeOthers() }
      return
    }

    val file = editorViewModel.getCurrentFile()
    var index = 0

    // keep closing the file at index 0
    // if openedFiles[0] == file, then keep closing files at index 1
    while (editorViewModel.getOpenedFileCount() != 1) {
      val editor = getEditorAtIndex(index)

      if (editor == null) {
        log.error("Unable to save file at index {}", index)
        continue
      }

      // Index of files changes as we keep close files
      // So we compare the files instead of index
      if (file != editor.file) {
        closeFile(index)
      } else {
        index = 1
      }
    }
  }

  override fun closeAll(runAfter: () -> Unit) {
    val count = editorViewModel.getOpenedFileCount()
    val unsavedFiles =
      editorViewModel.getOpenedFiles().map(this::getEditorForFile)
        .filter { it != null && it.isModified }

    if (unsavedFiles.isNotEmpty()) {
      // There are unsaved files
      notifyFilesUnsaved(unsavedFiles) { closeAll(runAfter) }
      return
    }

    // Files were already saved, close all files one by one
    for (i in 0 until count) {
      getEditorAtIndex(i)?.close() ?: run {
        log.error("Unable to close file at index {}", i)
      }
    }

    editorViewModel.removeAllFiles()
    content.apply {
      tabs.removeAllTabs()
      tabs.requestLayout()
      editorContainer.removeAllViews()
    }

    runAfter()
  }

  override fun getOpenedFiles() =
    editorViewModel.getOpenedFiles().mapNotNull {
      val editor = getEditorForFile(it)?.editor ?: return@mapNotNull null
      OpenedFile(it.absolutePath, editor.cursorLSPRange)
    }

  private fun notifyFilesUnsaved(unsavedEditors: List<CodeEditorView?>, invokeAfter: Runnable) {
    if (isDestroying) {
      // Do not show unsaved files dialog if the activity is being destroyed
      // TODO Use a service to save files and to avoid file content loss
      for (editor in unsavedEditors) {
        editor?.markUnmodified()
      }
      invokeAfter.run()
      return
    }

    val mapped = unsavedEditors.mapNotNull { it?.file?.absolutePath }
    val builder =
      newYesNoDialog(
        context = this,
        title = getString(string.title_files_unsaved),
        message = getString(string.msg_files_unsaved, TextUtils.join("\n", mapped)),
        positiveClickListener = { dialog, _ ->
          dialog.dismiss()
          saveAllAsync(notify = true, runAfter = { runOnUiThread(invokeAfter) })
        }
      ) { dialog, _ ->
        dialog.dismiss()
        // Mark all the files as saved, then try to close them all
        for (editor in unsavedEditors) {
          editor?.markAsSaved()
        }
        invokeAfter.run()
      }
    builder.show()
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onFileRenamed(event: FileRenameEvent) {
    val index = findIndexOfEditorByFile(event.file)
    if (index < 0 || index >= content.tabs.tabCount) {
      return
    }

    val editor = getEditorAtIndex(index) ?: return
    editorViewModel.updateFile(index, event.newFile)
    editor.updateFile(event.newFile)

    updateTabs()
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onDocumentChange(event: DocumentChangeEvent) {
    // This now serves as a trigger to re-evaluate the modification state for the whole UI.
    updateModificationState()

    val index = findIndexOfEditorByFile(event.file.toFile())
    if (index == -1) {
      return
    }

    val tab = content.tabs.getTabAt(index)!!
    val editorView = getEditorAtIndex(index)
    
    // Update the tab's text based on the new isModified state
    if (editorView?.isModified == true) {
        if (tab.text?.startsWith('*') == false) {
            tab.text = "*${tab.text}"
        }
    } else {
        if (tab.text?.startsWith('*') == true) {
            tab.text = tab.text!!.substring(startIndex = 1)
        }
    }
}

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onDocumentSaved(event: DocumentSaveEvent) {
    // When auto-save occurs, update the UI
    onFileSaved(event.file.toFile())
    // Also re-evaluate global modified state
    updateModificationState()
  }

  // Added to support Activity-level listener for preferences as requested
  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onPreferenceChanged(event: PreferenceChangeEvent) {
    // We primarily handle auto-save preference changes in CodeEditorView
    // But logging here allows debugging if settings propagate to the Activity level
    when (event.key) {
        EditorPreferences.AUTO_SAVE_ENABLED,
        EditorPreferences.AUTO_SAVE_DELAY_VALUE,
        EditorPreferences.AUTO_SAVE_DELAY_UNIT -> {
             log.debug("Activity received Auto-Save Preference change: Key=${event.key}, Value=${event.value}")
        }
    }
  }

  internal fun onFileModified(file: File?) {
    file ?: return
    val index = findIndexOfEditorByFile(file)
    if (index == -1) return
    val tab = content.tabs.getTabAt(index) ?: return
    val editorView = getEditorAtIndex(index)
    
    if (editorView?.isModified == true) {
        if (tab.text?.startsWith('*') == false) {
            tab.text = "*${tab.text}"
        }
    }
  }

  internal fun onFileSaved(file: File?) {
    file ?: return
    val index = findIndexOfEditorByFile(file)
    if (index == -1) return
    val tab = content.tabs.getTabAt(index) ?: return

    if (tab.text?.startsWith('*') == true) {
        tab.text = tab.text!!.substring(startIndex = 1)
    }
  }

  private fun updateTabs() {
    editorActivityScope.launch {
      val files = editorViewModel.getOpenedFiles()
      val dupliCount = mutableMapOf<String, Int>()
      // val names = MutableIntObjectMap<Pair<String, @DrawableRes Int>>()
      val names = MutableIntObjectMap<Pair<String, Int>>()
      val nameBuilder = UniqueNameBuilder<File>("", File.separator)

      files.forEach {
        var count = dupliCount[it.name] ?: 0
        dupliCount[it.name] = ++count
        nameBuilder.addPath(it, it.path)
      }

      for (index in 0 until content.tabs.tabCount) {
        val file = files.getOrNull(index) ?: continue
        val count = dupliCount[file.name] ?: 0

        val isModified = getEditorAtIndex(index)?.isModified == true
        var name = if (count > 1) nameBuilder.getShortPath(file) else file.name
        if (isModified) {
          name = "*$name"
        }

        names[index] = name to FileExtension.Factory.forFile(file).icon
      }

      withContext(Dispatchers.Main) {
        names.forEach { index, (name, iconId) ->
          val tab = content.tabs.getTabAt(index) ?: return@forEach
          tab.icon = ResourcesCompat.getDrawable(resources, iconId, theme)
          tab.text = name
        }
      }
    }
  }
}


/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.activities.editor

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.MarginLayoutParams
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.annotation.GravityInt
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.SizeUtils
import com.blankj.utilcode.util.ThreadUtils
import com.itsaky.androidide.R
import com.itsaky.androidide.R.string
import com.itsaky.androidide.databinding.LayoutSearchProjectBinding
import com.itsaky.androidide.flashbar.Flashbar
import com.itsaky.androidide.fragments.sheets.ProgressSheet
import com.itsaky.androidide.handlers.EditorBuildEventListener
import com.itsaky.androidide.handlers.LspHandler.connectClient
import com.itsaky.androidide.handlers.LspHandler.destroyLanguageServers
import com.itsaky.androidide.lookup.Lookup
import com.itsaky.androidide.lsp.IDELanguageClientImpl
import com.itsaky.androidide.lsp.java.utils.CancelChecker
import com.itsaky.androidide.preferences.internal.GeneralPreferences
import com.itsaky.androidide.projects.GradleProject
import com.itsaky.androidide.projects.builder.BuildService
import com.itsaky.androidide.projects.internal.ProjectManagerImpl
import com.itsaky.androidide.services.builder.GradleBuildService
import com.itsaky.androidide.services.builder.GradleBuildServiceConnnection
import com.itsaky.androidide.services.builder.gradleDistributionParams
import com.itsaky.androidide.tasks.executeAsyncProvideError
import com.itsaky.androidide.tasks.executeWithProgress
import com.itsaky.androidide.tooling.api.messages.AndroidInitializationParams
import com.itsaky.androidide.tooling.api.messages.InitializeProjectParams
import com.itsaky.androidide.tooling.api.messages.result.InitializeResult
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult.Failure.PROJECT_DIRECTORY_INACCESSIBLE
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult.Failure.PROJECT_NOT_DIRECTORY
import com.itsaky.androidide.tooling.api.messages.result.TaskExecutionResult.Failure.PROJECT_NOT_FOUND
import com.itsaky.androidide.tooling.api.models.BuildVariantInfo
import com.itsaky.androidide.tooling.api.models.mapToSelectedVariants
import com.itsaky.androidide.utils.DURATION_INDEFINITE
import com.itsaky.androidide.utils.DialogUtils.newMaterialDialogBuilder
import com.itsaky.androidide.utils.RecursiveFileSearcher
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.flashbarBuilder
import com.itsaky.androidide.utils.resolveAttr
import com.itsaky.androidide.utils.showOnUiThread
import com.itsaky.androidide.utils.withIcon
import com.itsaky.androidide.viewmodel.BuildVariantsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import java.util.stream.Collectors

/** @author Akash Yadav */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ProjectHandlerActivity : BaseEditorActivity() {

  protected val buildVariantsViewModel by viewModels<BuildVariantsViewModel>()

  protected var mSearchingProgress: ProgressSheet? = null
  protected var mFindInProjectDialog: AlertDialog? = null
  protected var syncNotificationFlashbar: Flashbar? = null

  protected var isFromSavedInstance = false
  protected var shouldInitialize = false

  protected var initializingFuture: CompletableFuture<out InitializeResult?>? = null

  val findInProjectDialog: AlertDialog
    get() {
      if (mFindInProjectDialog == null) {
        createFindInProjectDialog()
      }
      return mFindInProjectDialog!!
    }

  protected val mBuildEventListener = EditorBuildEventListener()

  private val buildServiceConnection = GradleBuildServiceConnnection()

  companion object {

    const val STATE_KEY_FROM_SAVED_INSTANACE = "ide.editor.isFromSavedInstance"
    const val STATE_KEY_SHOULD_INITIALIZE = "ide.editor.isInitializing"
  }

  abstract fun doCloseAll(runAfter: () -> Unit)

  abstract fun saveOpenedFiles()

  override fun doDismissSearchProgress() {
    if (mSearchingProgress?.isShowing == true) {
      mSearchingProgress!!.dismiss()
    }
  }

  override fun doConfirmProjectClose() {
    confirmProjectClose()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    savedInstanceState?.let {
      this.shouldInitialize = it.getBoolean(STATE_KEY_SHOULD_INITIALIZE, true)
      this.isFromSavedInstance = it.getBoolean(STATE_KEY_FROM_SAVED_INSTANACE, false)
    }
      ?: run {
        this.shouldInitialize = true
        this.isFromSavedInstance = false
      }

    editorViewModel._isSyncNeeded.observe(this) { isSyncNeeded ->
      if (!isSyncNeeded) {
        // dismiss if already showing
        syncNotificationFlashbar?.dismiss()
        return@observe
      }

      if (syncNotificationFlashbar?.isShowing() == true) {
        // already shown
        return@observe
      }

      notifySyncNeeded()
    }

    startServices()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.apply {
      putBoolean(STATE_KEY_SHOULD_INITIALIZE, !editorViewModel.isInitializing)
      putBoolean(STATE_KEY_FROM_SAVED_INSTANACE, true)
    }
  }

  override fun onPause() {
    super.onPause()
    if (isDestroying) {
      // reset these values here
      // sometimes, when the IDE closed and reopened instantly, these values prevent initialization
      // of the project
      ProjectManagerImpl.getInstance().destroy()

      editorViewModel.isInitializing = false
      editorViewModel.isBuildInProgress = false
    }
  }

  override fun preDestroy() {

    syncNotificationFlashbar?.dismiss()
    syncNotificationFlashbar = null

    if (isDestroying) {
      releaseServerListener()
      this.initializingFuture?.cancel(true)
      this.initializingFuture = null

      closeProject(false)
    }

    if (IDELanguageClientImpl.isInitialized()) {
      IDELanguageClientImpl.shutdown()
    }

    super.preDestroy()

    if (isDestroying) {

      try {
        stopLanguageServers()
      } catch (err: Exception) {
        log.error("Failed to stop editor services.")
      }

      try {
        unbindService(buildServiceConnection)
        buildServiceConnection.onConnected = {}
      } catch (err: Throwable) {
        log.error("Unable to unbind service")
      } finally {
        Lookup.getDefault().apply {

          (lookup(BuildService.KEY_BUILD_SERVICE) as? GradleBuildService?)
            ?.setEventListener(null)

          unregister(BuildService.KEY_BUILD_SERVICE)
        }

        mBuildEventListener.release()
        editorViewModel.isBoundToBuildSerice = false
      }
    }
  }

  fun setStatus(status: CharSequence) {
    setStatus(status, Gravity.CENTER)
  }

  fun setStatus(status: CharSequence, @GravityInt gravity: Int) {
    doSetStatus(status, gravity)
  }

  fun appendBuildOutput(str: String) {
    content.bottomSheet.appendBuildOut(str)
  }

  fun notifySyncNeeded() {
    notifySyncNeeded { initializeProject() }
  }

  private fun notifySyncNeeded(onConfirm: () -> Unit) {
    val buildService = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
    if (buildService == null || editorViewModel.isInitializing || buildService.isBuildInProgress) return

    this.syncNotificationFlashbar?.dismiss()

    this.syncNotificationFlashbar = flashbarBuilder(
      duration = DURATION_INDEFINITE,
      backgroundColor = resolveAttr(R.attr.colorSecondaryContainer),
      messageColor = resolveAttr(R.attr.colorOnSecondaryContainer)
    )
      .withIcon(R.drawable.ic_sync, colorFilter = resolveAttr(R.attr.colorOnSecondaryContainer))
      .message(string.msg_sync_needed)
      .positiveActionText(string.btn_sync)
      .positiveActionTapListener {
        onConfirm()
        it.dismiss()
      }
      .negativeActionText(string.btn_ignore_changes)
      .negativeActionTapListener(Flashbar::dismiss)
      .build()

    this.syncNotificationFlashbar?.showOnUiThread()

  }

  fun startServices() {
    val service = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE) as GradleBuildService?
    if (editorViewModel.isBoundToBuildSerice && service != null) {
      log.info("Reusing already started Gradle build service")
      onGradleBuildServiceConnected(service)
      return
    } else {
      log.info("Binding to Gradle build service...")
    }

    buildServiceConnection.onConnected = this::onGradleBuildServiceConnected

    if (
      bindService(
        Intent(this, GradleBuildService::class.java),
        buildServiceConnection,
        BIND_AUTO_CREATE or BIND_IMPORTANT
      )
    ) {
      log.info("Bind request for Gradle build service was successful...")
    } else {
      log.error("Gradle build service doesn't exist or the IDE is not allowed to access it.")
    }

    initLspClient()
  }

  /**
   * Initialize (sync) the project.
   *
   * @param buildVariantsProvider A function which returns the map of project paths to the selected build variants.
   *    This function is called asynchronously.
   */
  fun initializeProject(buildVariantsProvider: () -> Map<String, String>) {
    executeWithProgress { progress ->
      executeAsyncProvideError(buildVariantsProvider::invoke) { result, error ->
        com.itsaky.androidide.tasks.runOnUiThread {
          progress.dismiss()
        }

        if (result == null || error != null) {
          val msg = getString(string.msg_build_variants_fetch_failed)
          flashError(msg)
          log.error(msg, error)
          return@executeAsyncProvideError
        }

        com.itsaky.androidide.tasks.runOnUiThread {
          initializeProject(result)
        }
      }
    }
  }

  fun initializeProject() {
    val currentVariants = buildVariantsViewModel._buildVariants.value

    // no information about the build variants is available
    // use the default variant selections
    if (currentVariants == null) {
      log.debug(
        "No variant selection information available. Default build variants will be selected."
      )
      initializeProject(emptyMap())
      return
    }

    // variant selection information is available
    // but there are updated & unsaved variant selections
    // use the updated variant selections to initialize the project
    if (buildVariantsViewModel.updatedBuildVariants.isNotEmpty()) {
      val newSelections = currentVariants.toMutableMap()
      newSelections.putAll(buildVariantsViewModel.updatedBuildVariants)
      initializeProject {
        newSelections.mapToSelectedVariants().also {
          log.debug("Initializing project with new build variant selections: {}", it)
        }
      }
      return
    }

    // variant selection information is available but no variant selections have been updated
    // the user might be trying to sync the project from options menu
    // initialize the project with the existing selected variants
    initializeProject {
      log.debug("Re-initializing project with existing build variant selections")
      currentVariants.mapToSelectedVariants()
    }
  }

  /**
   * Initialize (sync) the project.
   *
   * @param buildVariants A map of project paths to the selected build variants.
   */
  fun initializeProject(buildVariants: Map<String, String>) {
    val manager = ProjectManagerImpl.getInstance()
    val projectDir = manager.projectDir
    if (!projectDir.exists()) {
      log.error("GradleProject directory does not exist. Cannot initialize project")
      return
    }

    val initialized = manager.projectInitialized && manager.cachedInitResult != null
    log.debug("Is project initialized: {}", initialized)
    // When returning after a configuration change between the initialization process,
    // we do not want to start another project initialization
    if (isFromSavedInstance && initialized && !shouldInitialize) {
      log.debug("Skipping init process because initialized && !wasInitializing")
      return
    }

    //noinspection ConstantConditions
    ThreadUtils.runOnUiThread { preProjectInit() }

    val buildService = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
    if (buildService == null) {
      log.error("No build service found. Cannot initialize project.")
      return
    }

    if (!buildService.isToolingServerStarted()) {
      flashError(string.msg_tooling_server_unavailable)
      return
    }

    this.initializingFuture =
      if (shouldInitialize || (!isFromSavedInstance && !initialized)) {
        log.debug("Sending init request to tooling server..")
        buildService.initializeProject(createProjectInitParams(projectDir, buildVariants))
      } else {
        // The project initialization was in progress before the configuration change
        // In this case, we should not start another project initialization
        log.debug("Using cached initialize result as the project is already initialized")
        CompletableFuture.supplyAsync {
          log.warn("GradleProject has already been initialized. Skipping initialization process.")
          manager.cachedInitResult
        }
      }

    this.initializingFuture!!.whenCompleteAsync { result, error ->
      releaseServerListener()

      if (result == null || !result.isSuccessful || error != null) {
        if (!CancelChecker.isCancelled(error)) {
          log.error("An error occurred initializing the project with Tooling API", error)
        }

        ThreadUtils.runOnUiThread {
          postProjectInit(false, result?.failure)
        }
        return@whenCompleteAsync
      }

      onProjectInitialized(result)
    }
  }

  private fun createProjectInitParams(
    projectDir: File,
    buildVariants: Map<String, String>
  ): InitializeProjectParams {
    return InitializeProjectParams(
      projectDir.absolutePath,
      gradleDistributionParams,
      createAndroidParams(buildVariants)
    )
  }

  private fun createAndroidParams(buildVariants: Map<String, String>): AndroidInitializationParams {
    if (buildVariants.isEmpty()) {
      return AndroidInitializationParams.DEFAULT
    }

    return AndroidInitializationParams(buildVariants)
  }

  private fun releaseServerListener() {
    // Release reference to server listener in order to prevent memory leak
    (Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE) as? GradleBuildService?)
      ?.setServerListener(null)
  }

  fun stopLanguageServers() {
    try {
      destroyLanguageServers(isChangingConfigurations)
    } catch (err: Throwable) {
      log.error("Unable to stop editor services. Please report this issue.", err)
    }
  }

  protected fun onGradleBuildServiceConnected(service: GradleBuildService) {
    log.info("Connected to Gradle build service")

    buildServiceConnection.onConnected = null
    editorViewModel.isBoundToBuildSerice = true
    Lookup.getDefault().update(BuildService.KEY_BUILD_SERVICE, service)
    service.setEventListener(mBuildEventListener)

    if (!service.isToolingServerStarted()) {
      service.startToolingServer { pid ->
        memoryUsageWatcher.watchProcess(pid, PROC_GRADLE_TOOLING)
        resetMemUsageChart()

        service.metadata().whenComplete { metadata, err ->
          if (metadata == null || err != null) {
            log.error("Failed to get tooling server metadata")
            return@whenComplete
          }

          if (pid != metadata.pid) {
            log.warn(
              "Tooling server pid mismatch. Expected: {}, Actual: {}. Replacing memory watcher...",
              pid, metadata.pid
            )
            memoryUsageWatcher.watchProcess(metadata.pid, PROC_GRADLE_TOOLING)
            resetMemUsageChart()
          }
        }

        initializeProject()
      }
    } else {
      initializeProject()
    }
  }

  protected open fun onProjectInitialized(result: InitializeResult) {
    val manager = ProjectManagerImpl.getInstance()
    if (isFromSavedInstance && manager.projectInitialized && result == manager.cachedInitResult) {
      log.debug("Not setting up project as this a configuration change")
      return
    }

    manager.cachedInitResult = result
    editorActivityScope.launch(Dispatchers.IO) {
      manager.setupProject()
      
      // Check if workspace setup was successful before accessing it
      val workspace = manager.getWorkspace()
      if (workspace == null) {
          log.error("Workspace setup failed. Workspace is null. Likely due to Tooling API model build failure.")
          com.itsaky.androidide.tasks.runOnUiThread {
              // Report failure to UI so it stops spinning
              postProjectInit(false, TaskExecutionResult.Failure.UNKNOWN)
          }
          return@launch
      }

      manager.notifyProjectUpdate()
      
      
      // Safe to call because we checked for null above
      updateBuildVariants(workspace.getAndroidVariantSelections())
       //The old method has been replaced by Line [515]
      // updateBuildVariants(manager.requireWorkspace().getAndroidVariantSelections())

      com.itsaky.androidide.tasks.runOnUiThread {
        postProjectInit(true, null)
      }
    }
  }

  protected open fun preProjectInit() {
    setStatus(getString(string.msg_initializing_project))
    editorViewModel.isInitializing = true
  }

  protected open fun postProjectInit(isSuccessful: Boolean, failure: TaskExecutionResult.Failure?) {
    val manager = ProjectManagerImpl.getInstance()
    if (!isSuccessful) {
      val initFailed = getString(string.msg_project_initialization_failed)
      setStatus(initFailed)

      val msg = when (failure) {
        PROJECT_DIRECTORY_INACCESSIBLE -> string.msg_project_dir_inaccessible
        PROJECT_NOT_DIRECTORY -> string.msg_file_is_not_dir
        PROJECT_NOT_FOUND -> string.msg_project_dir_doesnt_exist
        else -> null
      }?.let {
        "$initFailed: ${getString(it)}"
      }

      flashError(msg ?: initFailed)

      editorViewModel.isInitializing = false
      manager.projectInitialized = false
      return
    }

    initialSetup()
    setStatus(getString(string.msg_project_initialized))
    editorViewModel.isInitializing = false
    manager.projectInitialized = true

    if (mFindInProjectDialog?.isShowing == true) {
      mFindInProjectDialog!!.dismiss()
    }

    mFindInProjectDialog = null // Create the dialog again if needed
  }

  private fun updateBuildVariants(buildVariants: Map<String, BuildVariantInfo>) {
    // avoid using the 'runOnUiThread' method defined in the activity
    com.itsaky.androidide.tasks.runOnUiThread {
      buildVariantsViewModel.buildVariants = buildVariants
      buildVariantsViewModel.resetUpdatedSelections()
    }
  }

  protected open fun createFindInProjectDialog(): AlertDialog? {
    val manager = ProjectManagerImpl.getInstance()
    if (manager.getWorkspace() == null) {
      log.warn("No root project model found. Is the project initialized?")
      flashError(getString(string.msg_project_not_initialized))
      return null
    }

    val moduleDirs =
      try {
        manager.getWorkspace()!!.getSubProjects().stream().map(GradleProject::projectDir)
          .collect(Collectors.toList())
      } catch (e: Throwable) {
        flashError(getString(string.msg_no_modules))
        emptyList()
      }

    return createFindInProjectDialog(moduleDirs)
  }

  protected open fun createFindInProjectDialog(moduleDirs: List<File>): AlertDialog? {
    val srcDirs = mutableListOf<File>()
    val binding = LayoutSearchProjectBinding.inflate(layoutInflater)
    binding.modulesContainer.removeAllViews()

    for (i in moduleDirs.indices) {
      val module = moduleDirs[i]
      val src = File(module, "src")

      if (!module.exists() || !module.isDirectory || !src.exists() || !src.isDirectory) {
        continue
      }

      val check = CheckBox(this)
      check.text = module.name
      check.isChecked = true

      val params = MarginLayoutParams(-2, -2)
      params.bottomMargin = SizeUtils.dp2px(4f)
      binding.modulesContainer.addView(check, params)
      srcDirs.add(src)
    }

    val builder = newMaterialDialogBuilder(this)
    builder.setTitle(string.menu_find_project)
    builder.setView(binding.root)
    builder.setCancelable(false)
    builder.setPositiveButton(string.menu_find) { dialog, _ ->
      val text = binding.input.editText!!.text.toString().trim()
      if (text.isEmpty()) {
        flashError(string.msg_empty_search_query)
        return@setPositiveButton
      }

      val searchDirs = mutableListOf<File>()
      for (i in 0 until binding.modulesContainer.childCount) {
        val check = binding.modulesContainer.getChildAt(i) as CheckBox
        if (check.isChecked) {
          searchDirs.add(srcDirs[i])
        }
      }

      val extensions = binding.filter.editText!!.text.toString().trim()
      val extensionList = mutableListOf<String>()
      if (extensions.isNotEmpty()) {
        if (extensions.contains("|")) {
          for (str in
          extensions
            .split(Pattern.quote("|").toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            if (str.trim().isEmpty()) {
              continue
            }
            extensionList.add(str)
          }
        } else {
          extensionList.add(extensions)
        }
      }

      if (searchDirs.isEmpty()) {
        flashError(string.msg_select_search_modules)
      } else {
        dialog.dismiss()

        getProgressSheet(string.msg_searching_project)?.apply {
          show(supportFragmentManager, "search_in_project_progress")
        }

        RecursiveFileSearcher.searchRecursiveAsync(text, extensionList, searchDirs) { results ->
          handleSearchResults(results)
        }
      }
    }

    builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
    mFindInProjectDialog = builder.create()
    return mFindInProjectDialog
  }

  private fun initialSetup() {
    val manager = ProjectManagerImpl.getInstance()
    GeneralPreferences.lastOpenedProject = manager.projectDirPath
    try {
      val workspace = manager.getWorkspace()
      if (workspace == null) {
        log.warn("GradleProject not initialized. Skipping initial setup...")
        return
      }

      var projectName = workspace.getRootProject().name
      if (projectName.isEmpty()) {
        projectName = manager.projectDir.name
      }

      supportActionBar!!.subtitle = projectName
    } catch (th: Throwable) {
      // ignored
    }
  }

  private fun closeProject(manualFinish: Boolean) {
    if (manualFinish) {
      // if the user is manually closing the project,
      // save the opened files cache
      // this is needed because in this case, the opened files cache will be empty
      // when onPause will be called.
      saveOpenedFiles()

      // reset the lastOpenedProject if the user explicitly chose to close the project
      GeneralPreferences.lastOpenedProject = GeneralPreferences.NO_OPENED_PROJECT
    }

    // Make sure we close files
    // This will make sure that file contents are not erased.
    doCloseAll {
      if (manualFinish) {
        finish()
      }
    }
  }

  private fun confirmProjectClose() {
    val builder = newMaterialDialogBuilder(this)
    builder.setTitle(string.title_confirm_project_close)
    builder.setMessage(string.msg_confirm_project_close)
    builder.setNegativeButton(string.no, null)
    builder.setPositiveButton(string.yes) { dialog, _ ->
      dialog.dismiss()
      closeProject(true)
    }
    builder.show()
  }

  private fun initLspClient() {
    if (!IDELanguageClientImpl.isInitialized()) {
      IDELanguageClientImpl.initialize(this as EditorHandlerActivity)
    }
    connectClient(IDELanguageClientImpl.getInstance())
  }

  open fun getProgressSheet(msg: Int): ProgressSheet? {
    doDismissSearchProgress()

    mSearchingProgress =
      ProgressSheet().also {
        it.isCancelable = false
        it.setMessage(getString(msg))
        it.setSubMessageEnabled(false)
      }

    return mSearchingProgress
  }
}


/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.activities.editor

import android.content.Intent
import android.content.pm.PackageInstaller.SessionCallback
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Process
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.GravityInt
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.collection.MutableIntIntMap
import androidx.core.graphics.Insets
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import com.blankj.utilcode.constant.MemoryConstants
import com.blankj.utilcode.util.ConvertUtils.byte2MemorySize
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ThreadUtils
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.itsaky.androidide.R
import com.itsaky.androidide.R.string
import com.itsaky.androidide.actions.menu.EditorLineOperations
import com.itsaky.androidide.actions.ActionItem.Location.EDITOR_FILE_TABS
import com.itsaky.androidide.adapters.DiagnosticsAdapter
import com.itsaky.androidide.adapters.SearchListAdapter
import com.itsaky.androidide.app.EdgeToEdgeIDEActivity
import com.itsaky.androidide.databinding.ActivityEditorBinding
import com.itsaky.androidide.databinding.ContentEditorBinding
import com.itsaky.androidide.databinding.LayoutDiagnosticInfoBinding
import com.itsaky.androidide.events.InstallationResultEvent
import com.itsaky.androidide.fragments.SearchResultFragment
import com.itsaky.androidide.fragments.sidebar.EditorSidebarFragment
import com.itsaky.androidide.fragments.sidebar.FileTreeFragment
import com.itsaky.androidide.handlers.EditorActivityLifecyclerObserver
import com.itsaky.androidide.handlers.LspHandler.registerLanguageServers
import com.itsaky.androidide.interfaces.DiagnosticClickListener
import com.itsaky.androidide.lookup.Lookup
import com.itsaky.androidide.lsp.models.DiagnosticItem
import com.itsaky.androidide.models.DiagnosticGroup
import com.itsaky.androidide.models.OpenedFile
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.models.SearchResult
import com.itsaky.androidide.preferences.internal.BuildPreferences
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.tasks.cancelIfActive
import com.itsaky.androidide.ui.CodeEditorView
import com.itsaky.androidide.ui.ContentTranslatingDrawerLayout
import com.itsaky.androidide.ui.SwipeRevealLayout
import com.itsaky.androidide.uidesigner.UIDesignerActivity
import com.itsaky.androidide.utils.ActionMenuUtils.createMenu
import com.itsaky.androidide.utils.ApkInstallationSessionCallback
import com.itsaky.androidide.utils.DialogUtils.newMaterialDialogBuilder
import com.itsaky.androidide.utils.InstallationResultHandler.onResult
import com.itsaky.androidide.utils.IntentUtils
import com.itsaky.androidide.utils.MemoryUsageWatcher
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.resolveAttr
import com.itsaky.androidide.viewmodel.EditorViewModel
import com.itsaky.androidide.xml.resources.ResourceTableRegistry
import com.itsaky.androidide.xml.versions.ApiVersionsRegistry
import com.itsaky.androidide.xml.widgets.WidgetTableRegistry
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Base class for EditorActivity which handles most of the view related things.
 *
 * @author Akash Yadav
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseEditorActivity : EdgeToEdgeIDEActivity(), TabLayout.OnTabSelectedListener,
  DiagnosticClickListener {

  protected val mLifecycleObserver = EditorActivityLifecyclerObserver()
  protected var diagnosticInfoBinding: LayoutDiagnosticInfoBinding? = null
  protected var filesTreeFragment: FileTreeFragment? = null
  protected var editorBottomSheet: BottomSheetBehavior<out View?>? = null
  protected val memoryUsageWatcher = MemoryUsageWatcher()
  protected val pidToDatasetIdxMap = MutableIntIntMap(initialCapacity = 3)

  var isDestroying = false
    protected set

  /**
   * Editor activity's [CoroutineScope] for executing tasks in the background.
   */
  protected val editorActivityScope = CoroutineScope(Dispatchers.Default)

  internal var installationCallback: ApkInstallationSessionCallback? = null

  var uiDesignerResultLauncher: ActivityResultLauncher<Intent>? = null
  val editorViewModel by viewModels<EditorViewModel>()

  internal var _binding: ActivityEditorBinding? = null
  val binding: ActivityEditorBinding
    get() = checkNotNull(_binding) { "Activity has been destroyed" }
  val content: ContentEditorBinding
    get() = binding.content

  override val subscribeToEvents: Boolean
    get() = true

  private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      if (binding.root.isDrawerOpen(GravityCompat.START)) {
        binding.root.closeDrawer(GravityCompat.START)
      } else if (editorBottomSheet?.state != BottomSheetBehavior.STATE_COLLAPSED) {
        editorBottomSheet?.setState(BottomSheetBehavior.STATE_COLLAPSED)
      } else if (binding.swipeReveal.isOpen) {
        binding.swipeReveal.close()
      } else {
        doConfirmProjectClose()
      }
    }
  }

  private val memoryUsageListener = MemoryUsageWatcher.MemoryUsageListener { memoryUsage ->
    memoryUsage.forEachValue { proc ->
      _binding?.memUsageView?.chart?.apply {
        val dataset = (data.getDataSetByIndex(pidToDatasetIdxMap[proc.pid]) as LineDataSet?)
          ?: run {
            log.error("No dataset found for process: {}: {}", proc.pid, proc.pname)
            return@forEachValue
          }

        dataset.entries.mapIndexed { index, entry ->
          entry.y = byte2MemorySize(proc.usageHistory[index], MemoryConstants.MB).toFloat()
        }

        dataset.label = "%s - %.2fMB".format(proc.pname, dataset.entries.last().y)
        dataset.notifyDataSetChanged()
        data.notifyDataChanged()
        notifyDataSetChanged()
        invalidate()
      }
    }
  }

  private var isImeVisible = false
  private var contentCardRealHeight: Int? = null
  private val editorSurfaceContainerBackground by lazy {
    resolveAttr(R.attr.colorSurfaceDim)
  }
  private val editorLayoutCorners by lazy {
    resources.getDimensionPixelSize(R.dimen.editor_container_corners).toFloat()
  }

  private var optionsMenuInvalidator: Runnable? = null

  companion object {

    @JvmStatic
    protected val PROC_IDE = "IDE"

    @JvmStatic
    protected val PROC_GRADLE_TOOLING = "Gradle Tooling"

    @JvmStatic
    protected val PROC_GRADLE_DAEMON = "Gradle Daemon"

    @JvmStatic
    protected val log: Logger = LoggerFactory.getLogger(BaseEditorActivity::class.java)

    private const val OPTIONS_MENU_INVALIDATION_DELAY = 150L

    const val EDITOR_CONTAINER_SCALE_FACTOR = 0.87f
    const val KEY_BOTTOM_SHEET_SHOWN = "editor_bottomSheetShown"
    const val KEY_PROJECT_PATH = "saved_projectPath"
  }

  protected abstract fun provideCurrentEditor(): CodeEditorView?

  protected abstract fun provideEditorAt(index: Int): CodeEditorView?

  protected abstract fun doOpenFile(file: File, selection: Range?)

  protected abstract fun doDismissSearchProgress()

  protected abstract fun getOpenedFiles(): List<OpenedFile>

  internal abstract fun doConfirmProjectClose()

  protected open fun preDestroy() {
    _binding = null

    optionsMenuInvalidator?.also {
      ThreadUtils.getMainHandler().removeCallbacks(it)
    }

    optionsMenuInvalidator = null

    installationCallback?.destroy()
    installationCallback = null

    if (isDestroying) {
      memoryUsageWatcher.stopWatching(true)
      memoryUsageWatcher.listener = null
      editorActivityScope.cancelIfActive("Activity is being destroyed")
    }
  }

  protected open fun postDestroy() {
    if (isDestroying) {
      Lookup.getDefault().unregisterAll()
      ApiVersionsRegistry.getInstance().clear()
      ResourceTableRegistry.getInstance().clear()
      WidgetTableRegistry.getInstance().clear()
    }
  }

  override fun bindLayout(): View {
    this._binding = ActivityEditorBinding.inflate(layoutInflater)
    this.diagnosticInfoBinding = this.content.diagnosticInfo
    return this.binding.root
  }

  override fun onApplyWindowInsets(insets: WindowInsetsCompat) {
    super.onApplyWindowInsets(insets)
    val height = contentCardRealHeight ?: return
    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

    _binding?.content?.bottomSheet?.setImeVisible(imeInsets.bottom > 0)
    _binding?.contentCard?.updateLayoutParams<ViewGroup.LayoutParams> {
      this.height = height - imeInsets.bottom
    }

    val isImeVisible = imeInsets.bottom > 0
    if (this.isImeVisible != isImeVisible) {
      this.isImeVisible = isImeVisible
      onSoftInputChanged()
    }
  }

  override fun onApplySystemBarInsets(insets: Insets) {
    super.onApplySystemBarInsets(insets)
    this._binding?.apply {
      drawerSidebar.getFragment<EditorSidebarFragment>()
        .onApplyWindowInsets(insets)

      content.apply {
        editorAppBarLayout.updatePadding(
          top = insets.top
        )
        editorToolbar.updatePaddingRelative(
          start = editorToolbar.paddingStart + insets.left,
          end = editorToolbar.paddingEnd + insets.right
        )
      }
    }
  }

  @Subscribe(threadMode = MAIN)
  open fun onInstallationResult(event: InstallationResultEvent) {
    val intent = event.intent
    if (isDestroying) {
      return
    }

    val packageName = onResult(this, intent) ?: return

    if (BuildPreferences.launchAppAfterInstall) {
      IntentUtils.launchApp(this, packageName)
      return
    }

    Snackbar.make(content.realContainer, string.msg_action_open_application, Snackbar.LENGTH_LONG)
      .setAction(string.yes) { IntentUtils.launchApp(this, packageName) }.show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.optionsMenuInvalidator = Runnable { super.invalidateOptionsMenu() }

    registerLanguageServers()

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PROJECT_PATH)) {
      IProjectManager.getInstance()
        .openProject(savedInstanceState.getString(KEY_PROJECT_PATH)!!)
    }

    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    lifecycle.addObserver(mLifecycleObserver)

    setSupportActionBar(content.editorToolbar)

    setupDrawers()
    content.tabs.addOnTabSelectedListener(this)

    setupViews()

    setupContainers()
    setupDiagnosticInfo()

    uiDesignerResultLauncher = registerForActivityResult(
      StartActivityForResult(),
      this::handleUiDesignerResult
    )

    setupMemUsageChart()
    watchMemory()
  }

  private fun onSwipeRevealDragProgress(progress: Float) {
    _binding?.apply {
      contentCard.progress = progress
      val insetsTop = systemBarInsets?.top ?: 0
      content.editorAppBarLayout.updatePadding(
        top = (insetsTop * (1f - progress)).roundToInt()
      )
      memUsageView.chart.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = (insetsTop * progress).roundToInt()
      }
    }
  }

  private fun setupMemUsageChart() {
    binding.memUsageView.chart.apply {
      val colorAccent = resolveAttr(R.attr.colorAccent)

      isDragEnabled = false
      description.isEnabled = false
      xAxis.axisLineColor = colorAccent
      axisRight.axisLineColor = colorAccent

      setPinchZoom(false)
      setBackgroundColor(editorSurfaceContainerBackground)
      setDrawGridBackground(true)
      setScaleEnabled(true)

      axisLeft.isEnabled = false
      axisRight.valueFormatter = object :
        IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
          return "%dMB".format(value.roundToLong())
        }
      }
    }
  }

  private fun watchMemory() {
    memoryUsageWatcher.listener = memoryUsageListener
    memoryUsageWatcher.watchProcess(Process.myPid(), PROC_IDE)
    resetMemUsageChart()
  }

  protected fun resetMemUsageChart() {
    val processes = memoryUsageWatcher.getMemoryUsages()
    val datasets = Array(processes.size) { index ->
      LineDataSet(
        List(MemoryUsageWatcher.MAX_USAGE_ENTRIES) { Entry(it.toFloat(), 0f) },
        processes[index].pname
      )
    }

    val bgColor = editorSurfaceContainerBackground
    val textColor = resolveAttr(R.attr.colorOnSurface)

    for ((index, proc) in processes.withIndex()) {
      val dataset = datasets[index]
      dataset.color = getMemUsageLineColorFor(proc)
      dataset.setDrawIcons(false)
      dataset.setDrawCircles(false)
      dataset.setDrawCircleHole(false)
      dataset.setDrawValues(false)
      dataset.formLineWidth = 1f
      dataset.formSize = 15f
      dataset.isHighlightEnabled = false
      pidToDatasetIdxMap[proc.pid] = index
    }

    binding.memUsageView.chart.setBackgroundColor(bgColor)

    binding.memUsageView.chart.apply {
      data = LineData(*datasets)
      axisRight.textColor = textColor
      axisLeft.textColor = textColor
      legend.textColor = textColor

      data.setValueTextColor(textColor)
      setBackgroundColor(bgColor)
      setGridBackgroundColor(bgColor)
      notifyDataSetChanged()
      invalidate()
    }
  }

  private fun getMemUsageLineColorFor(proc: MemoryUsageWatcher.ProcessMemoryInfo): Int {
    return when (proc.pname) {
      PROC_IDE -> Color.BLUE
      PROC_GRADLE_TOOLING -> Color.RED
      PROC_GRADLE_DAEMON -> Color.GREEN
      else -> throw IllegalArgumentException("Unknown process: $proc")
    }
  }

  override fun onPause() {
    super.onPause()
    memoryUsageWatcher.listener = null
    memoryUsageWatcher.stopWatching(false)

    this.isDestroying = isFinishing
    getFileTreeFragment()?.saveTreeState()
  }

  override fun onResume() {
    super.onResume()
    invalidateOptionsMenu()

    memoryUsageWatcher.listener = memoryUsageListener
    memoryUsageWatcher.startWatching()

    try {
      getFileTreeFragment()?.listProjectFiles()
    } catch (th: Throwable) {
      log.error("Failed to update files list", th)
      flashError(string.msg_failed_list_files)
    }
  }

  override fun onStop() {
    super.onStop()
    checkIsDestroying()
  }

  override fun onDestroy() {
    checkIsDestroying()
    preDestroy()
    super.onDestroy()
    postDestroy()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putString(KEY_PROJECT_PATH, IProjectManager.getInstance().projectDirPath)
    super.onSaveInstanceState(outState)
  }

  override fun invalidateOptionsMenu() {
    val mainHandler = ThreadUtils.getMainHandler()
    optionsMenuInvalidator?.also {
      mainHandler.removeCallbacks(it)
      mainHandler.postDelayed(it, OPTIONS_MENU_INVALIDATION_DELAY)
    }
  }

  override fun onTabSelected(tab: Tab) {
    val position = tab.position
    editorViewModel.displayedFileIndex = position

    val editorView = provideEditorAt(position)!!
    // 中文注释: 每当一个标签被选中时，应用持久化的只读状态。
    // English annotation: Apply the persisted read-only state whenever a tab is selected.
    EditorLineOperations.applyReadOnlyState(editorView.editor!!, this)
    
    editorView.onEditorSelected()

    editorViewModel.setCurrentFile(position, editorView.file)
    refreshSymbolInput(editorView)
    invalidateOptionsMenu()
  }

  override fun onTabUnselected(tab: Tab) {}

  override fun onTabReselected(tab: Tab) {
    createMenu(this, tab.view, EDITOR_FILE_TABS, true).show()
  }

  override fun onGroupClick(group: DiagnosticGroup?) {
    if (group?.file?.exists() == true && FileUtils.isUtf8(group.file)) {
      doOpenFile(group.file, null)
      hideBottomSheet()
    }
  }

  override fun onDiagnosticClick(file: File, diagnostic: DiagnosticItem) {
    doOpenFile(file, diagnostic.range)
    hideBottomSheet()
  }

  open fun handleSearchResults(map: Map<File, List<SearchResult>>?) {
    val results = map ?: emptyMap()
    setSearchResultAdapter(SearchListAdapter(results, { file ->
      doOpenFile(file, null)
      hideBottomSheet()
    }) { match ->
      doOpenFile(match.file, match)
      hideBottomSheet()
    })

    showSearchResults()
    doDismissSearchProgress()
  }

  open fun setSearchResultAdapter(adapter: SearchListAdapter) {
    content.bottomSheet.setSearchResultAdapter(adapter)
  }

  open fun setDiagnosticsAdapter(adapter: DiagnosticsAdapter) {
    content.bottomSheet.setDiagnosticsAdapter(adapter)
  }

  open fun hideBottomSheet() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_COLLAPSED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
    }
  }

  open fun showSearchResults() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    val index = content.bottomSheet.pagerAdapter.findIndexOfFragmentByClass(
      SearchResultFragment::class.java
    )

    if (index >= 0 && index < content.bottomSheet.binding.tabs.tabCount) {
      content.bottomSheet.binding.tabs.getTabAt(index)?.select()
    }
  }

  open fun handleDiagnosticsResultVisibility(errorVisible: Boolean) {
    content.bottomSheet.handleDiagnosticsResultVisibility(errorVisible)
  }

  open fun handleSearchResultVisibility(errorVisible: Boolean) {
    content.bottomSheet.handleSearchResultVisibility(errorVisible)
  }

  open fun showFirstBuildNotice() {
    newMaterialDialogBuilder(this).setPositiveButton(android.R.string.ok, null)
      .setTitle(string.title_first_build).setMessage(string.msg_first_build).setCancelable(false)
      .create().show()
  }

  open fun getFileTreeFragment(): FileTreeFragment? {
    if (filesTreeFragment == null) {
      filesTreeFragment = supportFragmentManager.findFragmentByTag(
        FileTreeFragment.TAG
      ) as FileTreeFragment?
    }
    return filesTreeFragment
  }

  fun doSetStatus(text: CharSequence, @GravityInt gravity: Int) {
    editorViewModel.statusText = text
    editorViewModel.statusGravity = gravity
  }

  fun refreshSymbolInput() {
    provideCurrentEditor()?.also { refreshSymbolInput(it) }
  }

  fun refreshSymbolInput(editor: CodeEditorView) {
    content.bottomSheet.refreshSymbolInput(editor)
  }

  private fun checkIsDestroying() {
    if (!isDestroying && isFinishing) {
      isDestroying = true
    }
  }

  private fun handleUiDesignerResult(result: ActivityResult) {
    if (result.resultCode != RESULT_OK || result.data == null) {
      log.warn(
        "UI Designer returned invalid result: resultCode={}, data={}", result.resultCode,
        result.data
      )
      return
    }
    val generated = result.data!!.getStringExtra(UIDesignerActivity.RESULT_GENERATED_XML)
    if (TextUtils.isEmpty(generated)) {
      log.warn("UI Designer returned blank generated XML code")
      return
    }
    val view = provideCurrentEditor()
    val text = view?.editor?.text ?: run {
      log.warn("No file opened to append UI designer result")
      return
    }
    val endLine = text.lineCount - 1
    text.replace(0, 0, endLine, text.getColumnCount(endLine), generated)
  }

  private fun setupDrawers() {
    val toggle = ActionBarDrawerToggle(
      this, binding.editorDrawerLayout, content.editorToolbar,
      string.app_name, string.app_name
    )

    binding.editorDrawerLayout.addDrawerListener(toggle)
    toggle.syncState()
    binding.apply {
      editorDrawerLayout.apply {
        childId = contentCard.id
        translationBehaviorStart = ContentTranslatingDrawerLayout.TranslationBehavior.FULL
        translationBehaviorEnd = ContentTranslatingDrawerLayout.TranslationBehavior.FULL
        setScrimColor(Color.TRANSPARENT)
      }
    }
  }

  private fun onBuildStatusChanged() {
    log.debug(
      "onBuildStatusChanged: isInitializing: ${editorViewModel.isInitializing}, isBuildInProgress: ${editorViewModel.isBuildInProgress}"
    )
    val visible = editorViewModel.isBuildInProgress || editorViewModel.isInitializing
    content.progressIndicator.visibility = if (visible) View.VISIBLE else View.GONE
    invalidateOptionsMenu()
  }

  private fun setupViews() {
    editorViewModel._isBuildInProgress.observe(this) { onBuildStatusChanged() }
    editorViewModel._isInitializing.observe(this) { onBuildStatusChanged() }
    editorViewModel._statusText.observe(this) { content.bottomSheet.setStatus(it.first, it.second) }

    editorViewModel.observeFiles(this) { files ->
      content.apply {
        if (files.isNullOrEmpty()) {
          tabs.visibility = View.GONE
          viewContainer.displayedChild = 1
        } else {
          tabs.visibility = View.VISIBLE
          viewContainer.displayedChild = 0
        }
      }

      invalidateOptionsMenu()
    }

    setupNoEditorView()
    setupBottomSheet()

    if (!app.prefManager.getBoolean(
        KEY_BOTTOM_SHEET_SHOWN
      ) && editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED
    ) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
      ThreadUtils.runOnUiThreadDelayed({
        editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
        app.prefManager.putBoolean(KEY_BOTTOM_SHEET_SHOWN, true)
      }, 1500)
    }

    binding.contentCard.progress = 0f
    binding.swipeReveal.dragListener = object : SwipeRevealLayout.OnDragListener {
      override fun onDragStateChanged(swipeRevealLayout: SwipeRevealLayout, state: Int) {}
      override fun onDragProgress(swipeRevealLayout: SwipeRevealLayout, progress: Float) {
        onSwipeRevealDragProgress(progress)
      }
    }
  }

  private fun setupNoEditorView() {
    content.noEditorSummary.movementMethod = LinkMovementMethod()
    val filesSpan: ClickableSpan = object : ClickableSpan() {
      override fun onClick(widget: View) {
        binding.root.openDrawer(GravityCompat.START)
      }
    }
    val bottomSheetSpan: ClickableSpan = object : ClickableSpan() {
      override fun onClick(widget: View) {
        editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
      }
    }
    val sb = SpannableStringBuilder()
    appendClickableSpan(sb, string.msg_drawer_for_files, filesSpan)
    appendClickableSpan(sb, string.msg_swipe_for_output, bottomSheetSpan)
    content.noEditorSummary.text = sb
  }

  private fun appendClickableSpan(
    sb: SpannableStringBuilder,
    @StringRes textRes: Int,
    span: ClickableSpan,
  ) {
    val str = getString(textRes)
    val split = str.split("@@", limit = 3)
    if (split.size != 3) {
      // Not a valid format
      sb.append(str)
      sb.append('\n')
      return
    }
    sb.append(split[0])
    sb.append(split[1], span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.append(split[2])
    sb.append('\n')
  }

  private fun setupBottomSheet() {
    editorBottomSheet = BottomSheetBehavior.from<View>(content.bottomSheet)
    editorBottomSheet?.addBottomSheetCallback(object : BottomSheetCallback() {
      override fun onStateChanged(bottomSheet: View, newState: Int) {
        if (newState == BottomSheetBehavior.STATE_EXPANDED) {
          val editor = provideCurrentEditor()
          editor?.editor?.ensureWindowsDismissed()
        }
      }

      override fun onSlide(bottomSheet: View, slideOffset: Float) {
        content.apply {
          val editorScale = 1 - slideOffset * (1 - EDITOR_CONTAINER_SCALE_FACTOR)
          this.bottomSheet.onSlide(slideOffset)
          this.viewContainer.scaleX = editorScale
          this.viewContainer.scaleY = editorScale
        }
      }
    })

    val observer: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        contentCardRealHeight = binding.contentCard.height
        content.also {
          it.realContainer.pivotX = it.realContainer.width.toFloat() / 2f
          it.realContainer.pivotY =
            (it.realContainer.height.toFloat() / 2f) + (systemBarInsets?.run { bottom - top }
              ?: 0)
          it.viewContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
      }
    }

    content.apply {
      viewContainer.viewTreeObserver.addOnGlobalLayoutListener(observer)
      bottomSheet.setOffsetAnchor(editorAppBarLayout)
    }
  }

  private fun setupDiagnosticInfo() {
    val gd = GradientDrawable()
    gd.shape = GradientDrawable.RECTANGLE
    gd.setColor(-0xdededf)
    gd.setStroke(1, -0x1)
    gd.cornerRadius = 8f
    diagnosticInfoBinding?.root?.background = gd
    diagnosticInfoBinding?.root?.visibility = View.GONE
  }

  private fun setupContainers() {
    handleDiagnosticsResultVisibility(true)
    handleSearchResultVisibility(true)
  }

  private fun onSoftInputChanged() {
    if (!isDestroying) {
      invalidateOptionsMenu()
      content.bottomSheet.onSoftInputChanged()
    }
  }

  private fun showNeedHelpDialog() {
    val builder = newMaterialDialogBuilder(this)
    builder.setTitle(string.need_help)
    builder.setMessage(string.msg_need_help)
    builder.setPositiveButton(android.R.string.ok, null)
    builder.create().show()
  }

  open fun installationSessionCallback(): SessionCallback {
    return ApkInstallationSessionCallback(this).also { installationCallback = it }
  }
}

/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import com.blankj.utilcode.util.SizeUtils
import com.itsaky.androidide.activities.editor.BaseEditorActivity
import com.itsaky.androidide.app.BaseApplication
import com.itsaky.androidide.editor.api.IEditor
import com.itsaky.androidide.editor.databinding.LayoutCodeEditorBinding
import com.itsaky.androidide.editor.ui.EditorSearchLayout
import com.itsaky.androidide.editor.ui.IDEEditor
import com.itsaky.androidide.editor.ui.IDEEditor.Companion.createInputTypeFlags
import com.itsaky.androidide.editor.utils.ContentReadWrite.readContent
import com.itsaky.androidide.editor.utils.ContentReadWrite.writeTo
import com.itsaky.androidide.eventbus.events.preferences.PreferenceChangeEvent
import com.itsaky.androidide.lsp.IDELanguageClientImpl
import com.itsaky.androidide.lsp.api.ILanguageServer
import com.itsaky.androidide.lsp.api.ILanguageServerRegistry
import com.itsaky.androidide.lsp.java.JavaLanguageServer
import com.itsaky.androidide.lsp.xml.XMLLanguageServer
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.preferences.internal.EditorPreferences
import com.itsaky.androidide.syntax.colorschemes.SchemeAndroidIDE
import com.itsaky.androidide.tasks.cancelIfActive
import com.itsaky.androidide.tasks.runOnUiThread
import com.itsaky.androidide.utils.customOrJBMono
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.LineSeparator
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.Magnifier
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A view that handles opened code editor.
 *
 * @author Akash Yadav
 * @author android_zero
 */
@SuppressLint("ViewConstructor")
class CodeEditorView(
  context: Context,
  file: File,
  selection: Range
) : LinearLayoutCompat(context), Closeable {

  private var _binding: LayoutCodeEditorBinding? = null
  private var _searchLayout: EditorSearchLayout? = null

  private val codeEditorScope = CoroutineScope(
    Dispatchers.Default + CoroutineName("CodeEditorView"))
    
  private var autoSaveJob: Job? = null

  /**
   * The [CoroutineContext][kotlin.coroutines.CoroutineContext] used to reading and writing the file
   * in this editor. We use a separate, single-threaded context assuming that the file will be either
   * read from or written to at a time, but not both. If in future we add support for anything like
   * that, the number of thread should probably be increased.
   */
  @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
  private val readWriteContext = newSingleThreadContext("CodeEditorView")

  private val binding: LayoutCodeEditorBinding
    get() = checkNotNull(_binding) { "Binding has been destroyed" }

  private val searchLayout: EditorSearchLayout
    get() = checkNotNull(_searchLayout) { "Search layout has been destroyed" }

  /**
   * Get the file of this editor.
   */
  val file: File?
    get() = editor?.file

  /**
   * Get the [IDEEditor] instance of this editor view.
   */
  val editor: IDEEditor?
    get() = _binding?.editor

  /**
   * Returns whether the content of the editor has been modified.
   *
   * @see IDEEditor.isModified
   */
  val isModified: Boolean
    get() = editor?.isModified ?: false

  companion object {

    private val log = LoggerFactory.getLogger(CodeEditorView::class.java)
  }

  init {
    _binding = LayoutCodeEditorBinding.inflate(LayoutInflater.from(context))

    binding.editor.apply {
      isHighlightCurrentBlock = true
      props.autoCompletionOnComposing = true
      dividerWidth = SizeUtils.dp2px(2f).toFloat()
      colorScheme = SchemeAndroidIDE.newInstance(context)
      lineSeparator = LineSeparator.LF
      
      // Register content change listener for Auto-Save functionality (Delay based)
      subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
          scheduleAutoSave()
      }
      
      // Register focus change listener for Auto-Save on Focus Loss
      subscribeEvent(EditorFocusChangeEvent::class.java) { event, _ ->
          if (!event.isGainFocus && EditorPreferences.autoSaveOnFocusLoss && isModified) {
              log.debug("Focus lost. Performing auto-save for file: ${file?.name}")
              // Use codeEditorScope directly to launch save task
              codeEditorScope.launch { save() }
          }
      }
    }

    _searchLayout = EditorSearchLayout(context, binding.editor)

    orientation = VERTICAL

    removeAllViews()
    addView(binding.root, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    addView(searchLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

    readFileAndApplySelection(file, selection)
  }
  
  /**
   * Schedules an auto-save operation if enabled.
   * It cancels any pending auto-save job and starts a new one with the configured delay.
   */
  private fun scheduleAutoSave() {
      if (!EditorPreferences.autoSaveEnabled) return

      // Cancel previous job if active to debounce
      autoSaveJob?.cancel()

      val delayTime = calculateAutoSaveDelayMillis()
      
      autoSaveJob = codeEditorScope.launch {
          delay(delayTime)
          
          // Double check conditions before saving
          if (isModified && EditorPreferences.autoSaveEnabled) {
              log.debug("Performing delayed auto-save for file: ${file?.name}")
              save()
          }
      }
  }

  private fun calculateAutoSaveDelayMillis(): Long {
      val value = EditorPreferences.autoSaveDelayValue
      val unitStr = EditorPreferences.autoSaveDelayUnit
      // Assuming default is "Seconds" or "秒" if not matched to Minutes
      val isMinutes = unitStr.contains("Minute", ignoreCase = true) || unitStr.contains("分")
      
      return if (isMinutes) {
          TimeUnit.MINUTES.toMillis(value)
      } else {
          TimeUnit.SECONDS.toMillis(value)
      }
  }

  /**
   * Get the file of this editor. Throws [IllegalStateException] if no file is available.
   */
  fun requireFile(): File {
    return checkNotNull(file)
  }

  /**
   * Update the file of this editor. This only updates the file reference of the editor and does
   * not resets the content.
   */
  fun updateFile(file: File) {
    val editor = _binding?.editor ?: return
    editor.file = file
    postRead(file)
  }

  /**
   * Called when the editor has been selected and is visible to the user.
   */
  fun onEditorSelected() {
    _binding?.editor?.onEditorSelected() ?: run {
      log.warn("onEditorSelected() called but no editor instance is available")
    }
  }

  /**
   * Begins search mode and shows the [search layout][EditorSearchLayout].
   */
  fun beginSearch() {
    if (_binding == null || _searchLayout == null) {
      log.warn(
        "Editor layout is null content=$binding, searchLayout=$searchLayout")
      return
    }

    searchLayout.beginSearchMode()
  }

  /**
   * Mark this files as saved. Even if it not saved.
   */
  fun markAsSaved() {
    editor?.markUnmodified()
  }

  /**
   * Saves the content of the editor to the editor's file.
   *
   * @return Whether the save operation was successfully completed or not. If this method returns `false`,
   * it means that there was an error saving the file or the content of the file was not modified and
   * hence the save operation was skipped.
   */
  suspend fun save(): Boolean {
    val file = this.file ?: return false

    if (!isModified && file.exists()) {
      log.info("File was not modified. Skipping save operation for file {}", file.name)
      return false
    }

    val text = _binding?.editor?.text ?: run {
      log.error("Failed to save file. Unable to retrieve the content of editor as it is null.")
      return false
    }

    withContext(Dispatchers.Main.immediate) {

      withEditingDisabled {
        withContext(readWriteContext) {
          // Do not call suspend functions in this scope
          // the writeTo function acquires lock to the Content object before writing and releases
          // the lock after writing
          // if there are any suspend function calls in between, the lock and unlock calls might not
          // be called on the same thread
          text.writeTo(file, this@CodeEditorView::updateReadWriteProgress)
        }
      }

      _binding?.rwProgress?.isVisible = false
    }

    markUnmodified()
    notifySaved()

    return true
  }
  
  // private fun onSmoothCursorMovementChanged() {
    // binding.editor.isCursorAnimationEnabled = EditorPreferences.smoothCursorMovement
    
    // val smooth = EditorPreferences.smoothCursorMovement
    // val style = EditorPreferences.cursorStyle
    
    // if (smooth) {
        // if (style == "Block") {
            // binding.editor.setCursorAnimator(BlockCursorAnimator(binding.editor))
        // } else { // "Underline" or default
            // binding.editor.setCursorAnimator(MoveCursorAnimator(binding.editor))
        // }
    // } else {
        // binding.editor.isCursorAnimationEnabled = false
    // }
  // }
  
// private fun onAutoCompleteOnTypeChanged() {
      // binding.editor.props.autoCompletionOnComposing = EditorPreferences.autoCompleteOnType
  // }

  private fun updateReadWriteProgress(progress: Int) {
    val binding = this.binding
    runOnUiThread {
      if (binding.rwProgress.isVisible && (progress < 0 || progress >= 100)) {
        binding.rwProgress.isVisible = false
        return@runOnUiThread
      }

      if (!binding.rwProgress.isVisible) {
        binding.rwProgress.isVisible = true
      }

      binding.rwProgress.progress = progress
    }
  }

  private inline fun <R : Any?> withEditingDisabled(action: () -> R): R {
    return try {
      _binding?.editor?.isEditable = false
      action()
    } finally {
      _binding?.editor?.isEditable = true
    }
  }

  private fun readFileAndApplySelection(file: File, selection: Range) {
    codeEditorScope.launch(Dispatchers.Main.immediate) {
      updateReadWriteProgress(0)

      withEditingDisabled {

        val content = withContext(readWriteContext) {
          selection.validate()
          file.readContent(this@CodeEditorView::updateReadWriteProgress)
        }

        initializeContent(content, file, selection)
        _binding?.rwProgress?.isVisible = false
      }
    }
  }

  private fun initializeContent(content: Content, file: File, selection: Range) {
    val ideEditor = binding.editor
    ideEditor.postInLifecycle {
      val args = Bundle().apply {
        putString(IEditor.KEY_FILE, file.absolutePath)
      }

      ideEditor.setText(content, args)

      markUnmodified()
      postRead(file)

      ideEditor.validateRange(selection)
      ideEditor.setSelection(selection)

      configureEditorIfNeeded()
    }
  }

  private fun postRead(file: File) {
    binding.editor.setupLanguage(file)
    binding.editor.setLanguageServer(createLanguageServer(file))

    if (IDELanguageClientImpl.isInitialized()) {
      binding.editor.setLanguageClient(IDELanguageClientImpl.getInstance())
    }

    // File must be set only after setting the language server
    // This will make sure that textDocument/didOpen is sent
    binding.editor.file = file

    // do not pass this editor instance
    // symbol input must be updated for the current editor
    (context as? BaseEditorActivity?)?.refreshSymbolInput()
    (context as? Activity?)?.invalidateOptionsMenu()
  }

  private fun createLanguageServer(file: File): ILanguageServer? {
    if (!file.isFile) {
      return null
    }

    val serverID: String = when (file.extension) {
      "java" -> JavaLanguageServer.SERVER_ID
      "xml" -> XMLLanguageServer.SERVER_ID
      else -> return null
    }

    return ILanguageServerRegistry.getDefault().getServer(serverID)
  }

  private fun configureEditorIfNeeded() {
    onCustomFontPrefChanged()
    onFontSizePrefChanged()
    onFontLigaturesPrefChanged()
    onPrintingFlagsPrefChanged()
    onInputTypePrefChanged()
    onWordwrapPrefChanged()
    onMagnifierPrefChanged()
    onUseIcuPrefChanged()
    onDeleteEmptyLinesPrefChanged()
    onDeleteTabsPrefChanged()
    onStickyScrollEnabeldPrefChanged()
    onPinLineNumbersPrefChanged()
    // onSmoothCursorMovementChanged()
    // onAutoCompleteOnTypeChanged()
  }

  private fun onMagnifierPrefChanged() {
    binding.editor.getComponent(Magnifier::class.java).isEnabled = EditorPreferences.useMagnifier
  }

  private fun onWordwrapPrefChanged() {
    val enabled = EditorPreferences.wordwrap
    binding.editor.isWordwrap = enabled
  }

  private fun onInputTypePrefChanged() {
    binding.editor.inputType = createInputTypeFlags()
  }

  private fun onPrintingFlagsPrefChanged() {
    var flags = 0
    if (EditorPreferences.drawLeadingWs) {
      flags = flags or CodeEditor.FLAG_DRAW_WHITESPACE_LEADING
    }
    if (EditorPreferences.drawTrailingWs) {
      flags = flags or CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING
    }
    if (EditorPreferences.drawInnerWs) {
      flags = flags or CodeEditor.FLAG_DRAW_WHITESPACE_INNER
    }
    if (EditorPreferences.drawEmptyLineWs) {
      flags = flags or CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
    }
    if (EditorPreferences.drawLineBreak) {
      flags = flags or CodeEditor.FLAG_DRAW_LINE_SEPARATOR
    }
    binding.editor.nonPrintablePaintingFlags = flags
  }

  private fun onFontLigaturesPrefChanged() {
    val enabled = EditorPreferences.fontLigatures
    binding.editor.isLigatureEnabled = enabled
  }

  private fun onFontSizePrefChanged() {
    var textSize = EditorPreferences.fontSize
    if (textSize < 6 || textSize > 32) {
      textSize = 14f
    }
    binding.editor.setTextSize(textSize)
  }

  private fun onUseIcuPrefChanged() {
    binding.editor.props.useICULibToSelectWords = EditorPreferences.useIcu
  }

  private fun onCustomFontPrefChanged() {
    val state = EditorPreferences.useCustomFont
    binding.editor.typefaceText = customOrJBMono(state)
    binding.editor.typefaceLineNumber = customOrJBMono(state)
  }

  private fun onDeleteEmptyLinesPrefChanged() {
    binding.editor.props.deleteEmptyLineFast = EditorPreferences.deleteEmptyLines
  }

  private fun onDeleteTabsPrefChanged() {
    binding.editor.props.deleteMultiSpaces = if (EditorPreferences.deleteTabsOnBackspace) -1 else 1
  }

  private fun onStickyScrollEnabeldPrefChanged() {
    binding.editor.props.stickyScroll = EditorPreferences.stickyScrollEnabled
  }

  private fun onPinLineNumbersPrefChanged() {
    binding.editor.setPinLineNumber(EditorPreferences.pinLineNumbers)
  }

  /**
   * For internal use only!
   *
   *
   * Marks this editor as unmodified. Used only when the activity is being destroyed.
   */
  internal fun markUnmodified() {
    binding.editor.markUnmodified()
  }

  /**
   * For internal use only!
   *
   *
   * Marks this editor as modified.
   */
  internal fun markModified() {
    binding.editor.markModified()
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  @Suppress("unused")
  fun onPreferenceChanged(event: PreferenceChangeEvent) {
    if (_binding == null) {
      return
    }

    BaseApplication.getBaseInstance().prefManager
    when (event.key) {
      EditorPreferences.FONT_SIZE -> onFontSizePrefChanged()
      EditorPreferences.FONT_LIGATURES -> onFontLigaturesPrefChanged()

      EditorPreferences.FLAG_LINE_BREAK,
      EditorPreferences.FLAG_WS_INNER,
      EditorPreferences.FLAG_WS_EMPTY_LINE,
      EditorPreferences.FLAG_WS_LEADING,
      EditorPreferences.FLAG_WS_TRAILING -> onPrintingFlagsPrefChanged()

      EditorPreferences.FLAG_PASSWORD -> onInputTypePrefChanged()
      EditorPreferences.WORD_WRAP -> onWordwrapPrefChanged()
      EditorPreferences.USE_MAGNIFER -> onMagnifierPrefChanged()
      EditorPreferences.USE_ICU -> onUseIcuPrefChanged()
      EditorPreferences.USE_CUSTOM_FONT -> onCustomFontPrefChanged()
      EditorPreferences.DELETE_EMPTY_LINES -> onDeleteEmptyLinesPrefChanged()
      EditorPreferences.DELETE_TABS_ON_BACKSPACE -> onDeleteTabsPrefChanged()
      EditorPreferences.STICKY_SCROLL_ENABLED -> onStickyScrollEnabeldPrefChanged()
      EditorPreferences.PIN_LINE_NUMBERS -> onPinLineNumbersPrefChanged()
      
      // Auto-Save Preference Changes
      EditorPreferences.AUTO_SAVE_ENABLED -> {
          if (!EditorPreferences.autoSaveEnabled) {
              autoSaveJob?.cancel()
          }
      }
      EditorPreferences.AUTO_SAVE_DELAY_VALUE,
      EditorPreferences.AUTO_SAVE_DELAY_UNIT -> {
          // If currently running a delay, restart it with new value
          if (autoSaveJob?.isActive == true) {
              scheduleAutoSave()
          }
      }
    }
  }

  /**
   * Notifies the editor that its content has been saved.
   */
  private fun notifySaved() {
    binding.editor.dispatchDocumentSaveEvent()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (!EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().register(this)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    EventBus.getDefault().unregister(this)
    autoSaveJob?.cancel()
  }

  override fun close() {
    codeEditorScope.cancelIfActive("Cancellation was requested")
    autoSaveJob?.cancel()
    
    _binding?.editor?.apply {
      notifyClose()
      release()
    }

    readWriteContext.use { }
  }
}


/*
 * This file is part of AndroidIDE.
 *
 *
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package com.itsaky.androidide.lsp;

import static com.itsaky.androidide.resources.R.drawable;
import static com.itsaky.androidide.resources.R.string;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.itsaky.androidide.activities.editor.EditorHandlerActivity;
import com.itsaky.androidide.adapters.DiagnosticsAdapter;
import com.itsaky.androidide.adapters.SearchListAdapter;
import com.itsaky.androidide.editor.ui.IDEEditor;
import com.itsaky.androidide.fragments.sheets.ProgressSheet;
import com.itsaky.androidide.lsp.api.ILanguageClient;
import com.itsaky.androidide.lsp.models.CodeActionItem;
import com.itsaky.androidide.lsp.models.DiagnosticItem;
import com.itsaky.androidide.lsp.models.DiagnosticResult;
import com.itsaky.androidide.lsp.models.PerformCodeActionParams;
import com.itsaky.androidide.lsp.models.ShowDocumentParams;
import com.itsaky.androidide.lsp.models.ShowDocumentResult;
import com.itsaky.androidide.lsp.models.TextEdit;
import com.itsaky.androidide.lsp.util.DiagnosticUtil;
import com.itsaky.androidide.models.DiagnosticGroup;
import com.itsaky.androidide.models.Location;
import com.itsaky.androidide.models.Range;
import com.itsaky.androidide.models.SearchResult;
import com.itsaky.androidide.tasks.TaskExecutor;
import com.itsaky.androidide.ui.CodeEditorView;
import com.itsaky.androidide.utils.FlashbarActivityUtilsKt;
import com.itsaky.androidide.utils.FlashbarUtilsKt;
import com.itsaky.androidide.utils.LSPUtils;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.text.Content;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AndroidIDE specific implementation of the LanguageClient
 */
public class IDELanguageClientImpl implements ILanguageClient {

  public static final int MAX_DIAGNOSTIC_FILES = 10;
  public static final int MAX_DIAGNOSTIC_ITEMS_PER_FILE = 20;
  protected static final Logger LOG = LoggerFactory.getLogger(IDELanguageClientImpl.class);
  private static IDELanguageClientImpl mInstance;
  private final Map<File, List<DiagnosticItem>> diagnostics = new HashMap<>();
  protected EditorHandlerActivity activity;

  private IDELanguageClientImpl(EditorHandlerActivity provider) {
    setActivity(provider);
  }

  public void setActivity(EditorHandlerActivity provider) {
    this.activity = provider;
  }

  public static IDELanguageClientImpl initialize(EditorHandlerActivity provider) {
    if (mInstance != null) {
      throw new IllegalStateException("Client is already initialized");
    }

    mInstance = new IDELanguageClientImpl(provider);

    return getInstance();
  }

  public static IDELanguageClientImpl getInstance() {
    if (mInstance == null) {
      throw new IllegalStateException("Client not initialized");
    }

    return mInstance;
  }

  public static void shutdown() {
    if (mInstance != null) {
      mInstance.activity = null;
    }
    mInstance = null;
  }

  public static boolean isInitialized() {
    return mInstance != null;
  }

  @Override
  public void publishDiagnostics(DiagnosticResult result) {
    if (result == DiagnosticResult.NO_UPDATE || !canUseActivity()) {
      // No update is expected
      return;
    }

    boolean error = result == null;
    activity.handleDiagnosticsResultVisibility(error || result.getDiagnostics().isEmpty());

    if (error) {
      return;
    }

    File file = result.getFile().toFile();
    if (!file.exists() || !file.isFile()) {
      return;
    }

    final var editorView = activity.getEditorForFile(file);
    if (editorView != null) {
      final var editor = editorView.getEditor();
      if (editor != null) {
        final var container = new DiagnosticsContainer();
        try {
          container.addDiagnostics(
              result.getDiagnostics().stream()
                  .map(DiagnosticItem::asDiagnosticRegion)
                  .collect(Collectors.toList()));
        } catch (Throwable err) {
          LOG.error("Unable to map DiagnosticItem to DiagnosticRegion", err);
        }
        editor.setDiagnostics(container);
      }
    }

    diagnostics.put(file, result.getDiagnostics());
    activity.setDiagnosticsAdapter(newDiagnosticsAdapter());
  }

  @Nullable
  @Override
  public DiagnosticItem getDiagnosticAt(final File file, final int line, final int column) {
    return DiagnosticUtil.binarySearchDiagnostic(this.diagnostics.get(file), line, column);
  }

  @Override
  public void performCodeAction(PerformCodeActionParams params) {
    if (params == null) {
      return;
    }

    final var action = params.getAction();
    if (!canUseActivity()) {
      LOG.error("Unable to perform code action activity=null action={}", action);
      FlashbarUtilsKt.flashError(string.msg_cannot_perform_fix);
      return;
    }

    final var currentEditor = this.activity.getCurrentEditor();
    final var editor = currentEditor != null ? currentEditor.getEditor() : null;

    if (!params.getAsync()) {
      applyActionEdits(editor, action);
      if (editor != null) {
        action.getCommand();
        editor.executeCommand(action.getCommand());
      }
      return;
    }

    final ProgressSheet progress = new ProgressSheet();
    progress.setSubMessageEnabled(false);
    progress.setCancelable(false);
    progress.setMessage(this.activity.getString(string.msg_performing_actions));
    progress.show(this.activity.getSupportFragmentManager(), "quick_fix_progress");

    TaskExecutor.executeAsyncProvideError(
        () -> applyActionEdits(editor, action),
        (result, throwable) -> {
          progress.dismiss();
          if (result == null || throwable != null || !result) {
            LOG.error("Unable to perform code action result={}", result, throwable);
            FlashbarActivityUtilsKt.flashError(this.activity, string.msg_cannot_perform_fix);
          } else if (editor != null) {
            editor.executeCommand(action.getCommand());
          }
        });
  }

  private Boolean applyActionEdits(@Nullable final IDEEditor editor, final CodeActionItem action) {
    final var changes = action.getChanges();
    if (changes.isEmpty()) {
      return Boolean.FALSE;
    }

    for (var change : changes) {
      final var path = change.getFile();
      if (path == null) {
        continue;
      }

      final File file = path.toFile();
      if (!file.exists()) {
        continue;
      }

      for (TextEdit edit : change.getEdits()) {
        final String editorFilepath =
            editor == null || editor.getFile() == null ? "" : editor.getFile().getAbsolutePath();
        if (file.getAbsolutePath().equals(editorFilepath)) {
          // Edit is in the same editor which requested the code action
          editInEditor(editor, edit);
        } else {
          var openedFrag = findEditorByFile(file);

          if (openedFrag != null && openedFrag.getEditor() != null) {
            // Edit is in another 'opened' file
            editInEditor(openedFrag.getEditor(), edit);
          } else {
            // Edit is in some other file which is not opened
            // open that file and perform the edit
            openedFrag = activity.openFile(file);
            if (openedFrag != null && openedFrag.getEditor() != null) {
              editInEditor(openedFrag.getEditor(), edit);
            }
          }
        }
      }
    }

    return Boolean.TRUE;
  }

  private void editInEditor(final IDEEditor editor, final TextEdit edit) {
    activity
        .runOnUiThread(
            () -> {
              final Range range = edit.getRange();
              final int startLine = range.getStart().getLine();
              final int startCol = range.getStart().getColumn();
              final int endLine = range.getEnd().getLine();
              final int endCol = range.getEnd().getColumn();
              if (startLine == endLine && startCol == endCol) {
                editor.getText().insert(startLine, startCol, edit.getNewText());
              } else {
                editor.getText().replace(startLine, startCol, endLine, endCol, edit.getNewText());
              }
            });
  }

  @Override
  public ShowDocumentResult showDocument(ShowDocumentParams params) {
    boolean success = false;
    final var result = new ShowDocumentResult(false);
    if (!canUseActivity()) {
      return result;
    }

    if (params != null) {
      File file = params.getFile().toFile();
      if (file.exists() && file.isFile() && FileUtils.isUtf8(file)) {
        final var range = params.getSelection();
        var frag =
            activity.getEditorAtIndex(activity.getContent().tabs.getSelectedTabPosition());
        if (frag != null
            && frag.getFile() != null
            && frag.getEditor() != null
            && frag.getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
          if (LSPUtils.isEqual(range.getStart(), range.getEnd())) {
            frag.getEditor().setSelection(range.getStart().getLine(), range.getStart().getColumn());
          } else {
            frag.getEditor().setSelection(range);
          }
        } else {
          activity.openFileAndSelect(file, range);
        }
        success = true;
      }
    }

    result.setSuccess(success);
    return result;
  }

  public DiagnosticsAdapter newDiagnosticsAdapter() {
    return new DiagnosticsAdapter(mapAsGroup(this.diagnostics), activity);
  }

  private List<DiagnosticGroup> mapAsGroup(Map<File, List<DiagnosticItem>> map) {
    final var groups = new ArrayList<DiagnosticGroup>();
    var diagnosticMap = map;
    if (diagnosticMap == null || diagnosticMap.size() == 0) {
      return groups;
    }

    if (diagnosticMap.size() > 10) {
      LOG.warn("Limiting the diagnostics to 10 files");
      diagnosticMap = filterRelevantDiagnostics(map);
    }

    for (File file : diagnosticMap.keySet()) {
      var fileDiagnostics = diagnosticMap.get(file);
      if (fileDiagnostics == null || fileDiagnostics.size() == 0) {
        continue;
      }

      // Trim the diagnostics list if we have too many diagnostic items.
      // Including a lot of diagnostic items will result in UI lag when they are shown
      if (fileDiagnostics.size() > MAX_DIAGNOSTIC_ITEMS_PER_FILE) {
        LOG.warn("Limiting diagnostics to {} items for file {}",
            MAX_DIAGNOSTIC_ITEMS_PER_FILE,
            file.getName());

        fileDiagnostics = fileDiagnostics.subList(0, MAX_DIAGNOSTIC_ITEMS_PER_FILE);
      }
      DiagnosticGroup group = new DiagnosticGroup(drawable.ic_language_java, file, fileDiagnostics);
      groups.add(group);
    }
    return groups;
  }

  @NonNull
  private Map<File, List<DiagnosticItem>> filterRelevantDiagnostics(
      @NonNull final Map<File, List<DiagnosticItem>> map) {
    final var result = new HashMap<File, List<DiagnosticItem>>();
    final var files = map.keySet();

    // Diagnostics of files that are open must always be included
    final var relevantFiles = findOpenFiles(files, MAX_DIAGNOSTIC_FILES);

    // If we can show a few more file diagnostics...
    if (relevantFiles.size() < MAX_DIAGNOSTIC_FILES) {
      final var alphabetical = new TreeSet<>(Comparator.comparing(File::getName));
      alphabetical.addAll(files);
      for (var file : alphabetical) {
        relevantFiles.add(file);
        if (relevantFiles.size() == MAX_DIAGNOSTIC_FILES) {
          break;
        }
      }
    }

    for (var file : relevantFiles) {
      result.put(file, map.get(file));
    }
    return result;
  }

  @NonNull
  private Set<File> findOpenFiles(final Set<File> files, final int max) {
    final var openedFiles = activity.getEditorViewModel().getOpenedFiles();
    final var result = new TreeSet<File>();
    for (int i = 0; i < openedFiles.size(); i++) {
      final var opened = openedFiles.get(i);
      if (files.contains(opened)) {
        result.add(opened);
      }
      if (result.size() == max) {
        break;
      }
    }
    return result;
  }

  /**
   * Called by {@link IDEEditor IDEEditor} to show locations in EditorActivity
   */
  @Override
  public void showLocations(List<Location> locations) {

    // Cannot show anything if the activity() is null
    if (!canUseActivity()) {
      return;
    }

    boolean error = locations == null || locations.isEmpty();
    activity.handleSearchResultVisibility(error);

    if (error) {
      activity
          .setSearchResultAdapter(
              new SearchListAdapter(Collections.emptyMap(), this::noOp, this::noOp));
      return;
    }

    final Map<File, List<SearchResult>> results = new HashMap<>();
    for (int i = 0; i < locations.size(); i++) {
      try {
        final Location loc = locations.get(i);
        if (loc == null) {
          continue;
        }

        final File file = loc.getFile().toFile();
        if (!file.exists() || !file.isFile()) {
          continue;
        }
        var frag = findEditorByFile(file);
        Content content;
        if (frag != null && frag.getEditor() != null) {
          content = frag.getEditor().getText();
        } else {
          content = new Content(FileIOUtils.readFile2String(file));
        }
        final List<SearchResult> matches =
            results.containsKey(file) ? results.get(file) : new ArrayList<>();
        Objects.requireNonNull(matches)
            .add(
                new SearchResult(
                    loc.getRange(),
                    file,
                    content.getLineString(loc.getRange().getStart().getLine()),
                    content
                        .subContent(
                            loc.getRange().getStart().getLine(),
                            loc.getRange().getStart().getColumn(),
                            loc.getRange().getEnd().getLine(),
                            loc.getRange().getEnd().getColumn())
                        .toString()));
        results.put(file, matches);
      } catch (Throwable th) {
        LOG.error("Failed to show file location", th);
      }
    }

    activity.handleSearchResults(results);
  }

  private CodeEditorView findEditorByFile(File file) {
    return activity.getEditorForFile(file);
  }

  private boolean canUseActivity() {
    return activity != null
        && !activity.isFinishing()
        && !activity.isDestroyed()
        && !activity.getSupportFragmentManager().isDestroyed()
        && !activity.getSupportFragmentManager().isStateSaved();
  }

  private Unit noOp(final Object obj) {
    return Unit.INSTANCE;
  }
}


/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.editor.ui

import com.itsaky.androidide.editor.api.IEditor
import com.itsaky.androidide.editor.ui.IDEEditor.Companion.log
import com.itsaky.androidide.models.Position
import com.itsaky.androidide.models.Range
import io.github.rosemoe.sora.widget.SelectionMovement
import java.io.File

/**
 * Handler which implements various features in [IEditor].
 *
 * @author Akash Yadav
 * @author android_zero
 */
class EditorFeatures(
  var editor: IDEEditor? = null
) : IEditor {

  override fun getFile(): File? = withEditor { _file }

  /**
   * 动态检查文件是否自上次保存后被修改过。
   *
   * @return 如果内容已更改，则为 true；否则为 false。
   */
  override fun isModified(): Boolean = withEditor { isModified() } ?: false

  override fun setSelection(position: Position) {
    withEditor {
      setSelection(position.line, position.column)
    }
  }

  override fun setSelection(start: Position, end: Position) {
    withEditor {
      if (!isValidPosition(start, true) || !isValidPosition(end, true)) {
        log.warn("Invalid selection range: start={} end={}", start, end)
        return@withEditor
      }

      setSelectionRegion(start.line, start.column, end.line, end.column)
    }
  }

  override fun setSelectionAround(line: Int, column: Int) {
    withEditor {
      if (line < lineCount) {
        val columnCount = text.getColumnCount(line)
        setSelection(line, if (column > columnCount) columnCount else column)
      } else {
        setSelection(lineCount - 1, text.getColumnCount(lineCount - 1))
      }
    }
  }

  override fun getCursorLSPRange(): Range = withEditor {
    val end = cursor.right().let {
      Position(line = it.line, column = it.column, index = it.index)
    }
    return@withEditor Range(cursorLSPPosition, end)
  } ?: Range.NONE

  override fun getCursorLSPPosition(): Position = withEditor {
    return@withEditor cursor.left().let {
      Position(line = it.line, column = it.column, index = it.index)
    }
  } ?: Position.NONE

  override fun validateRange(range: Range) {
    withEditor {
      val start = range.start
      val end = range.end
      val text = text
      val lineCount = text.lineCount

      start.line = 0.coerceAtLeast(start.line).coerceAtMost(lineCount - 1)
      start.column = 0.coerceAtLeast(start.column).coerceAtMost(text.getColumnCount(start.line))

      end.line = 0.coerceAtLeast(end.line).coerceAtMost(lineCount - 1)
      end.column = 0.coerceAtLeast(end.column).coerceAtMost(text.getColumnCount(end.line))
    }
  }

  override fun isValidRange(range: Range?, allowColumnEqual: Boolean): Boolean = withEditor {
    if (range == null) {
      return@withEditor false
    }
    val start = range.start
    val end = range.end
    return@withEditor isValidPosition(start, allowColumnEqual)
        // make sure start position is before end position
        && isValidPosition(end, allowColumnEqual) && start < end
  } ?: false

  override fun isValidPosition(position: Position?, allowColumnEqual: Boolean): Boolean =
    withEditor {
      return@withEditor if (position == null) {
        false
      } else isValidLine(position.line) &&
          isValidColumn(position.line, position.column, allowColumnEqual)
    } ?: false

  override fun isValidLine(line: Int): Boolean =
    withEditor { line >= 0 && line < text.lineCount } ?: false

  override fun isValidColumn(line: Int, column: Int, allowColumnEqual: Boolean): Boolean =
    withEditor {
      val columnCount = text.getColumnCount(line)
      return@withEditor column >= 0 && (column < columnCount || allowColumnEqual && column == columnCount)
    } ?: false

  override fun append(text: CharSequence?): Int = withEditor {
    val content = getText()
    if (lineCount <= 0) {
      return@withEditor 0
    }

    val line = lineCount - 1
    var col = content.getColumnCount(line)
    if (col < 0) {
      col = 0
    }
    content.insert(line, col, text)
    return@withEditor line
  } ?: -1

  override fun replaceContent(newContent: CharSequence?) {
    withEditor {
      val lastLine = text.lineCount - 1
      val lastColumn = text.getColumnCount(lastLine)
      text.replace(0, 0, lastLine, lastColumn, newContent ?: "")
    }
  }

  override fun goToEnd() {
    withEditor {
      moveSelection(SelectionMovement.TEXT_END)
    }
  }

  private inline fun <T> withEditor(crossinline action: IDEEditor.() -> T): T? {
    return this.editor?.run {
      if (isReleased) {
        null
      } else action()
    }
  }
}


/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.itsaky.androidide.editor.ui

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.SizeUtils
import com.itsaky.androidide.editor.R.string
import com.itsaky.androidide.editor.adapters.CompletionListAdapter
import com.itsaky.androidide.editor.api.IEditor
import com.itsaky.androidide.editor.api.ILspEditor
import com.itsaky.androidide.editor.language.IDELanguage
import com.itsaky.androidide.editor.language.cpp.CppLanguage
import com.itsaky.androidide.editor.language.groovy.GroovyLanguage
import com.itsaky.androidide.editor.language.treesitter.TreeSitterLanguage
import com.itsaky.androidide.editor.language.treesitter.TreeSitterLanguageProvider
import com.itsaky.androidide.editor.schemes.IDEColorScheme
import com.itsaky.androidide.editor.schemes.IDEColorSchemeProvider
import com.itsaky.androidide.editor.snippets.AbstractSnippetVariableResolver
import com.itsaky.androidide.editor.snippets.FileVariableResolver
import com.itsaky.androidide.editor.snippets.WorkspaceVariableResolver
import com.itsaky.androidide.eventbus.events.editor.ChangeType
import com.itsaky.androidide.eventbus.events.editor.ColorSchemeInvalidatedEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentChangeEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentCloseEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentOpenEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentSaveEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentSelectedEvent
import com.itsaky.androidide.flashbar.Flashbar
import com.itsaky.androidide.lsp.api.ILanguageClient
import com.itsaky.androidide.lsp.api.ILanguageServer
import com.itsaky.androidide.lsp.java.utils.CancelChecker
import com.itsaky.androidide.lsp.models.Command
import com.itsaky.androidide.lsp.models.DefinitionParams
import com.itsaky.androidide.lsp.models.DefinitionResult
import com.itsaky.androidide.lsp.models.ExpandSelectionParams
import com.itsaky.androidide.lsp.models.ReferenceParams
import com.itsaky.androidide.lsp.models.ReferenceResult
import com.itsaky.androidide.lsp.models.ShowDocumentParams
import com.itsaky.androidide.lsp.models.SignatureHelp
import com.itsaky.androidide.lsp.models.SignatureHelpParams
import com.itsaky.androidide.models.Position
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.preferences.internal.EditorPreferences
import com.itsaky.androidide.progress.ICancelChecker
import com.itsaky.androidide.syntax.colorschemes.DynamicColorScheme
import com.itsaky.androidide.syntax.colorschemes.SchemeAndroidIDE
import com.itsaky.androidide.tasks.JobCancelChecker
import com.itsaky.androidide.tasks.cancelIfActive
import com.itsaky.androidide.tasks.launchAsyncWithProgress
import com.itsaky.androidide.utils.DocumentUtils
import com.itsaky.androidide.utils.flashError
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.UndoManager
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.IDEEditorSearcher
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Field

/**
 * [CodeEditor] implementation for the IDE.
 *
 * @author Akash Yadav
 * @author android_zero
 */
open class IDEEditor @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
  private val editorFeatures: EditorFeatures = EditorFeatures()
) : CodeEditor(context, attrs, defStyleAttr, defStyleRes), IEditor by editorFeatures, ILspEditor {

  /**
   * A callback interface to notify the host (usually an Activity) about file modification status changes.
   * This decouples the editor component from the specific Activity that hosts it.
   */
  interface ModificationCallback {
      fun onFileModified(file: File?)
      fun onFileSaved(file: File?)
  }
    
  @Suppress("PropertyName")
  internal var _file: File? = null

  private var _actionsMenu: EditorActionsMenu? = null
  private var _signatureHelpWindow: SignatureHelpWindow? = null
  private var _diagnosticWindow: DiagnosticWindow? = null
  private var fileVersion = 0

  private var mLastSaveHistoryIndex: Int = 0
  private var mUndoManagerField: Field? = null
    
  private val selectionChangeHandler = Handler(Looper.getMainLooper())
  private var selectionChangeRunner: Runnable? = Runnable {
    val languageClient = languageClient ?: return@Runnable
    val cursor = this.cursor ?: return@Runnable

    if (cursor.isSelected || _signatureHelpWindow?.isShowing == true) {
      return@Runnable
    }

    diagnosticWindow.showDiagnostic(
      languageClient.getDiagnosticAt(file, cursor.leftLine, cursor.leftColumn))
  }

  /**
   * The [CoroutineScope] for the editor.
   *
   * All the jobs in this scope are cancelled when the editor is released.
   */
  val editorScope = CoroutineScope(Dispatchers.Default + CoroutineName("IDEEditor"))

  protected val eventDispatcher = EditorEventDispatcher()

  private var setupTsLanguageJob: Job? = null
  private var sigHelpCancelChecker: ICancelChecker? = null

  var languageServer: ILanguageServer? = null
    private set

  var languageClient: ILanguageClient? = null
    private set

  /**
   * Whether the cursor position change animation is enabled for the editor.
   */
  var isEnsurePosAnimEnabled = true

  /**
   * The text searcher for the editor.
   */
  lateinit var searcher: IDEEditorSearcher

  /**
   * The signature help window for the editor.
   */
  val signatureHelpWindow: SignatureHelpWindow
    get() {
      return _signatureHelpWindow ?: SignatureHelpWindow(this).also { _signatureHelpWindow = it }
    }

  /**
   * The diagnostic window for the editor.
   */
  val diagnosticWindow: DiagnosticWindow
    get() {
      return _diagnosticWindow ?: DiagnosticWindow(this).also { _diagnosticWindow = it }
    }

  companion object {

    private const val SELECTION_CHANGE_DELAY = 500L

    internal val log = LoggerFactory.getLogger(IDEEditor::class.java)

    /**
     * Create input type flags for the editor.
     */
    fun createInputTypeFlags(): Int {
      var flags = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
      if (EditorPreferences.visiblePasswordFlag) {
        flags = flags or EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
      }
      return flags
    }
  }

  init {
    run {
      editorFeatures.editor = this
      eventDispatcher.editor = this
      eventDispatcher.init(editorScope)
      initEditor()
    }
  }

  /**
   * Set the file for this editor.
   */
  fun setFile(file: File?) {
    if (isReleased) {
      return
    }

    this._file = file
    file?.also {
      dispatchDocumentOpenEvent()
    }
  }

  override fun setLanguageServer(server: ILanguageServer?) {
    if (isReleased) {
      return
    }
    this.languageServer = server
    server?.also {
      this.languageClient = it.client
      snippetController.apply {
        fileVariableResolver = FileVariableResolver(this@IDEEditor)
        workspaceVariableResolver = WorkspaceVariableResolver()
      }
    }
  }


  override fun setLanguageClient(client: ILanguageClient?) {
    if (isReleased) {
      return
    }
    this.languageClient = client
  }

  override fun executeCommand(command: Command?) {
    if (isReleased) {
      return
    }
    if (command == null) {
      log.warn("Cannot execute command in editor. Command is null.")
      return
    }

    log.info(String.format("Executing command '%s' for completion item.", command.title))
    when (command.command) {
      Command.TRIGGER_COMPLETION -> {
        val completion = getComponent(EditorAutoCompletion::class.java)
        completion.requireCompletion()
      }

      Command.TRIGGER_PARAMETER_HINTS -> signatureHelp()
      Command.FORMAT_CODE -> formatCodeAsync()
    }
  }

  override fun signatureHelp() {
    if (isReleased) {
      return
    }
    val languageServer = this.languageServer ?: return
    val file = this.file ?: return

    this.languageClient ?: return

    sigHelpCancelChecker?.also { it.cancel() }

    val cancelChecker = JobCancelChecker().also {
      this.sigHelpCancelChecker = it
    }

    editorScope.launch(Dispatchers.Default) {
      cancelChecker.job = coroutineContext[Job]

      val help = safeGet("signature help request") {
        val params = SignatureHelpParams(file.toPath(), cursorLSPPosition, cancelChecker)
        languageServer.signatureHelp(params)
      }

      withContext(Dispatchers.Main) {
        showSignatureHelp(help)
      }
    }.logError("signature help request")
  }

  override fun showSignatureHelp(help: SignatureHelp?) {
    if (isReleased) {
      return
    }
    signatureHelpWindow.setupAndDisplay(help)
  }

  override fun findDefinition() {
    if (isReleased) {
      return
    }
    val languageServer = this.languageServer ?: return
    val file = file ?: return

    launchCancellableAsyncWithProgress(string.msg_finding_definition) { _, cancelChecker ->
      val result = safeGet("definition request") {
        val params = DefinitionParams(file.toPath(), cursorLSPPosition, cancelChecker)
        languageServer.findDefinition(params)
      }

      onFindDefinitionResult(result)
    }?.logError("definition request")
  }

  override fun findReferences() {
    if (isReleased) {
      return
    }
    val languageServer = this.languageServer ?: return
    val file = file ?: return

    launchCancellableAsyncWithProgress(string.msg_finding_references) { _, cancelChecker ->
      val result = safeGet("references request") {
        val params = ReferenceParams(file.toPath(), cursorLSPPosition, true, cancelChecker)
        languageServer.findReferences(params)
      }

      onFindReferencesResult(result)
    }?.logError("references request")
  }

  override fun expandSelection() {
    if (isReleased) {
      return
    }
    val languageServer = this.languageServer ?: return
    val file = file ?: return

    launchCancellableAsyncWithProgress(string.please_wait) { _, _ ->
      val initialRange = cursorLSPRange
      val result = safeGet("expand selection request") {
        val params = ExpandSelectionParams(file.toPath(), initialRange)
        languageServer.expandSelection(params)
      } ?: initialRange

      withContext(Dispatchers.Main) {
        setSelection(result)
      }
    }?.logError("expand selection request")
  }

  override fun ensureWindowsDismissed() {
    if (_diagnosticWindow?.isShowing == true) {
      _diagnosticWindow?.dismiss()
    }

    if (_signatureHelpWindow?.isShowing == true) {
      _signatureHelpWindow?.dismiss()
    }

    if (_actionsMenu?.isShowing == true) {
      _actionsMenu?.dismiss()
    }
  }

  final override fun <T : EditorBuiltinComponent?> replaceComponent(clazz: Class<T>, replacement: T & Any) {
    super.replaceComponent(clazz, replacement)
  }

  // not overridable
  final override fun <T : EditorBuiltinComponent?> getComponent(clazz: Class<T>): T & Any {
    return super.getComponent(clazz)
  }

  override fun release() {
    ensureWindowsDismissed()

    if (isReleased) {
      return
    }

    super.release()

    snippetController.apply {
      (fileVariableResolver as? AbstractSnippetVariableResolver?)?.close()
      (workspaceVariableResolver as? AbstractSnippetVariableResolver?)?.close()

      fileVariableResolver = null
      workspaceVariableResolver = null
    }

    _actionsMenu?.destroy()

    _actionsMenu = null
    _signatureHelpWindow = null
    _diagnosticWindow = null

    languageServer = null
    languageClient = null

    _file = null
    fileVersion = 0
    markUnmodified()

    editorFeatures.editor = null
    eventDispatcher.editor = null

    eventDispatcher.destroy()

    selectionChangeRunner?.also { selectionChangeHandler.removeCallbacks(it) }
    selectionChangeRunner = null

    if (EventBus.getDefault().isRegistered(this)) {
      EventBus.getDefault().unregister(this)
    }

    setupTsLanguageJob?.cancel("Editor is releasing resources.")

    if (editorScope.isActive) {
      editorScope.cancelIfActive("Editor is releasing resources.")
    }
  }

  override fun getSearcher(): EditorSearcher {
    return this.searcher
  }

  override fun getExtraArguments(): Bundle {
    return super.getExtraArguments().apply {
      putString(IEditor.KEY_FILE, file?.absolutePath)
    }
  }

  override fun ensurePositionVisible(line: Int, column: Int, noAnimation: Boolean) {
    super.ensurePositionVisible(line, column, !isEnsurePosAnimEnabled || noAnimation)
  }

  override fun getTabWidth(): Int {
    return EditorPreferences.tabSize
  }

  override fun beginSearchMode() {
    throw UnsupportedOperationException("Search ActionMode is not supported. Use CodeEditorView.beginSearch() instead.")
  }

  override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    if (!gainFocus) {
      ensureWindowsDismissed()
    }
  }

  /**
   * Analyze the opened file and publish the diagnostics result.
   */
  open fun analyze() {
    if (isReleased) {
      return
    }
    if (editorLanguage !is IDELanguage) {
      return
    }

    val languageServer = languageServer ?: return
    val file = file ?: return

    editorScope.launch {
      val result = safeGet("LSP file analysis") { languageServer.analyze(file.toPath()) }
      languageClient?.publishDiagnostics(result)
    }.logError("LSP file analysis")
  }

   fun markUnmodified() {
    if (isReleased || text == null) {
      return
    }
    mLastSaveHistoryIndex = getHistoryIndex(text.undoManager)
    (context as? ModificationCallback)?.onFileSaved(this.file)
  }

   fun markModified() {
    (context as? ModificationCallback)?.onFileModified(this.file)
  }

  override fun isModified(): Boolean {
    return if (isReleased || text == null) {
      false
    } else {
      getHistoryIndex(text.undoManager) != mLastSaveHistoryIndex
    }
  }

  /**
   * Notify the language server that the file in this editor is about to be closed.
   */
  open fun notifyClose() {
    if (isReleased) {
      return
    }
    file ?: run {
      log.info("Cannot notify language server. File is null.")
      return
    }

    dispatchDocumentCloseEvent()

    _actionsMenu?.unsubscribeEvents()
    selectionChangeRunner?.also {
      selectionChangeHandler.removeCallbacks(it)
    }

    selectionChangeRunner = null

    ensureWindowsDismissed()
  }

  /**
   * Called when this editor is selected and visible to the user.
   */
  open fun onEditorSelected() {
    if (isReleased) {
      return
    }

    file ?: return
    dispatchDocumentSelectedEvent()
  }

  /**
   * Dispatches the [DocumentSaveEvent] for this editor.
   */
  open fun dispatchDocumentSaveEvent() {
    markUnmodified()
    if (isReleased) {
      return
    }
    if (file == null) {
      return
    }
    eventDispatcher.dispatch(DocumentSaveEvent(file!!.toPath()))
  }

  /**
   * Called when the color scheme has been invalidated. This usually happens when the user reloads
   * the color schemes.
   */
  @Suppress("unused")
  @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
  open fun onColorSchemeInvalidated(event: ColorSchemeInvalidatedEvent?) {
    val file = file ?: return
    setupLanguage(file)
  }

  /**
   * Setup the editor language for the given [file].
   *
   * This applies a proper [Language] and the color scheme to the editor.
   */
  open fun setupLanguage(file: File?) {
    if (isReleased) {
      return
    }
    if (file == null) {
      return
    }

    createLanguage(file) { language ->
      val extension = file.extension
      if (language is TreeSitterLanguage) {
        IDEColorSchemeProvider.readSchemeAsync(context = context, coroutineScope = editorScope,
          type = extension) { scheme ->
          applyTreeSitterLang(language, extension, scheme)
        }
      } else {
        setEditorLanguage(language)
      }
    }
  }

  /**
   * Applies the given [TreeSitterLanguage] and the [color scheme][scheme] for the given [file type][type].
   */
  open fun applyTreeSitterLang(language: TreeSitterLanguage, type: String,
    scheme: SchemeAndroidIDE?) {
    applyTreeSitterLangInternal(language, type, scheme)
  }

  private fun applyTreeSitterLangInternal(language: TreeSitterLanguage, type: String,
    scheme: SchemeAndroidIDE?) {
    if (isReleased) {
      return
    }
    var finalScheme = if (scheme != null) {
      scheme
    } else {
      log.error("Failed to read current color scheme")
      SchemeAndroidIDE.newInstance(context)
    }

    if (finalScheme is IDEColorScheme) {

      language.setupWith(finalScheme)

      if (finalScheme.getLanguageScheme(type) == null) {
        log.warn("Color scheme does not support file type '{}'", type)
        finalScheme = SchemeAndroidIDE.newInstance(context)
      }
    }

    if (finalScheme is DynamicColorScheme) {
      finalScheme.apply(context)
    }

    colorScheme = finalScheme!!
    setEditorLanguage(language)
  }

  private inline fun createLanguage(file: File, crossinline callback: (Language?) -> Unit) {

    // 1 -> If the given File object does not represent a file, return emtpy language
    if (!file.isFile) {
      return callback(EmptyLanguage())
    }

    // 2 -> In case a TreeSitterLanguage has been registered for this file type,
    //      Initialize the TreeSitterLanguage asynchronously and then invoke the callback
    if (TreeSitterLanguageProvider.hasTsLanguage(file)) {

      // lazily create TS languages as they need to read files from assets
      setupTsLanguageJob = editorScope.launch {
        callback(TreeSitterLanguageProvider.forFile(file, context))
      }.also { job ->
        job.invokeOnCompletion { err ->
          if (err != null) {
            log.error("Failed to setup tree sitter language for file: {}", file, err)
          }

          setupTsLanguageJob = null
        }
      }

      return
    }

    // 3 -> Check if we have ANTLR4 lexer-based languages for this file,
    //      return the language if we do, otherwise return an empty language
    val lang = when (FileUtils.getFileExtension(file)) {
      "gradle" -> GroovyLanguage()
      "c", "h", "cc", "cpp", "cxx" -> CppLanguage()
      else -> EmptyLanguage()
    }

    callback(lang)
  }

  /**
   * Initialize the editor.
   */
  protected open fun initEditor() {

    lineNumberMarginLeft = SizeUtils.dp2px(2f).toFloat()

    _actionsMenu = EditorActionsMenu(this).also {
      it.init()
    }

    markUnmodified()

    searcher = IDEEditorSearcher(this)
    colorScheme = SchemeAndroidIDE.newInstance(context)
    inputType = createInputTypeFlags()

    val window = EditorCompletionWindow(this)
    window.setAdapter(CompletionListAdapter())
    replaceComponent(EditorAutoCompletion::class.java, window)

    getComponent(EditorTextActionWindow::class.java).isEnabled = false

    subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
      if (isReleased) {
        return@subscribeEvent
      }

      // 历史索引会自动改变，isModified() 方法现在可以动态地、准确地反映状态。
      // 我们只需要通知UI（如Activity）内容已变更，以便它可以更新文件标签（例如添加星号 '*'）
      (context as? ModificationCallback)?.onFileModified(this.file)
      
      file ?: return@subscribeEvent

      editorScope.launch {
        dispatchDocumentChangeEvent(event)
        checkForSignatureHelp(event)
      }
    }

    subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
      if (isReleased) {
        return@subscribeEvent
      }

      if (_diagnosticWindow?.isShowing == true) {
        _diagnosticWindow?.dismiss()
      }

      selectionChangeRunner?.also {
        selectionChangeHandler.removeCallbacks(it)
        selectionChangeHandler.postDelayed(it, SELECTION_CHANGE_DELAY)
      }
    }

    EventBus.getDefault().register(this)
  }

private fun getHistoryIndex(undoManager: UndoManager): Int {
    if (mUndoManagerField == null) {
      try {
        mUndoManagerField = undoManager.javaClass.getDeclaredField("stackPointer").apply {
          isAccessible = true
        }
      } catch (e: NoSuchFieldException) {
        log.error("Failed to get 'stackPointer' field from UndoManager", e)
        return -1
      }
    }
    return try {
      mUndoManagerField!!.getInt(undoManager)
    } catch (e: Exception) {
      log.error("Failed to get value of 'stackPointer' from UndoManager", e)
      -1
    }
  }
  
  private inline fun launchCancellableAsyncWithProgress(@StringRes message: Int,
    crossinline action: suspend CoroutineScope.(flashbar: Flashbar, cancelChecker: ICancelChecker) -> Unit): Job? {
    if (isReleased) {
      return null
    }

    return editorScope.launchAsyncWithProgress(configureFlashbar = { builder, cancelChecker ->
      configureFlashbar(builder, message, cancelChecker)
    }, action = action)
  }

  protected open suspend fun onFindDefinitionResult(
    result: DefinitionResult?,
  ) = withContext(Dispatchers.Main) {

    if (isReleased) {
      return@withContext
    }

    val languageClient = languageClient ?: run {
      log.error("No language client found to handle the definitions result")
      return@withContext
    }

    if (result == null) {
      log.error("Invalid definitions result from language server (null)")
      flashError(string.msg_no_definition)
      return@withContext
    }

    val locations = result.locations
    if (locations.isEmpty()) {
      log.error("No definitions found")
      flashError(string.msg_no_definition)
      return@withContext
    }

    if (locations.size != 1) {
      languageClient.showLocations(locations)
      return@withContext
    }

    val (file1, range) = locations[0]
    if (DocumentUtils.isSameFile(file1, file!!.toPath())) {
      setSelection(range)
      return@withContext
    }

    languageClient.showDocument(ShowDocumentParams(file1, range))
  }

  protected open suspend fun onFindReferencesResult(result: ReferenceResult?) =
    withContext(Dispatchers.Main) {

      if (isReleased) {
        return@withContext
      }

      val languageClient = languageClient ?: run {
        log.error("No language client found to handle the references result")
        return@withContext
      }

      if (result == null) {
        log.error("Invalid references result from language server (null)")
        flashError(string.msg_no_references)
        return@withContext
      }

      val locations = result.locations
      if (locations.isEmpty()) {
        log.error("No references found")
        flashError(string.msg_no_references)
        return@withContext
      }

      if (result.locations.size == 1) {
        val (file, range) = result.locations[0]

        if (DocumentUtils.isSameFile(file, getFile()!!.toPath())) {
          setSelection(range)
          return@withContext
        }
      }

      languageClient.showLocations(result.locations)
    }

  protected open fun dispatchDocumentOpenEvent() {
    if (isReleased) {
      return
    }

    val file = this.file ?: return

    this.fileVersion = 0

    val openEvent = DocumentOpenEvent(file.toPath(), text.toString(), fileVersion)

    eventDispatcher.dispatch(openEvent)
  }

  protected open fun dispatchDocumentChangeEvent(event: ContentChangeEvent) {
    if (isReleased) {
      return
    }

    val file = file?.toPath() ?: return
    var type = ChangeType.INSERT
    if (event.action == ContentChangeEvent.ACTION_DELETE) {
      type = ChangeType.DELETE
    } else if (event.action == ContentChangeEvent.ACTION_SET_NEW_TEXT) {
      type = ChangeType.NEW_TEXT
    }
    var changeDelta = if (type == ChangeType.NEW_TEXT) 0 else event.changedText.length
    if (type == ChangeType.DELETE) {
      changeDelta = -changeDelta
    }
    val start = event.changeStart
    val end = event.changeEnd
    val changeRange = Range(Position(start.line, start.column, start.index),
      Position(end.line, end.column, end.index))
    val changedText = event.changedText.toString()
    val changeEvent = DocumentChangeEvent(file, changedText, text.toString(), ++fileVersion, type,
      changeDelta, changeRange)

    eventDispatcher.dispatch(changeEvent)
  }

  protected open fun dispatchDocumentSelectedEvent() {
    if (isReleased) {
      return
    }
    val file = file ?: return
    eventDispatcher.dispatch(DocumentSelectedEvent(file.toPath()))
  }

  protected open fun dispatchDocumentCloseEvent() {
    if (isReleased) {
      return
    }
    val file = file ?: return

    eventDispatcher.dispatch(DocumentCloseEvent(file.toPath(), cursorLSPRange))
  }

  /**
   * Checks if the content change event should trigger signature help. Signature help trigger
   * characters are :
   *
   *
   *  * `'('` (parentheses)
   *  * `','` (comma)
   *
   *
   * @param event The content change event.
   */
  private fun checkForSignatureHelp(event: ContentChangeEvent) {
    if (isReleased) {
      return
    }
    if (languageServer == null) {
      return
    }
    val changeLength = event.changedText.length
    if (event.action != ContentChangeEvent.ACTION_INSERT || changeLength < 1 || changeLength > 2) {
      // change length will be 1 if ',' is inserted
      // changeLength will be 2 as '(' and ')' are inserted at the same time
      return
    }

    val ch = event.changedText[0]
    if (ch == '(' || ch == ',') {
      signatureHelp()
    }
  }

  private fun configureFlashbar(builder: Flashbar.Builder, @StringRes message: Int,
    cancelChecker: ICancelChecker) {
    builder.message(message).primaryActionText(android.R.string.cancel)
      .primaryActionTapListener { bar: Flashbar ->
        cancelChecker.cancel()
        bar.dismiss()
      }
  }

  private inline fun <T> safeGet(name: String, action: () -> T): T? {
    return try {
      action()
    } catch (err: Throwable) {
      logError(err, name)
      null
    }
  }

  private fun Job.logError(action: String): Job = apply {
    invokeOnCompletion { err -> logError(err, action) }
  }

  private fun logError(err: Throwable?, action: String) {
    err ?: return
    if (CancelChecker.isCancelled(err)) {
      log.warn("{} has been cancelled", action)
    } else {
      log.error("{} failed", action)
    }
  }

  override fun setSelectionAround(line: Int, column: Int) {
    editorFeatures.setSelectionAround(line, column)
  }
}


