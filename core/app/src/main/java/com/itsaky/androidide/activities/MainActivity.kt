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

package com.itsaky.androidide.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.transition.TransitionManager
import androidx.transition.doOnEnd
import com.google.android.material.transition.MaterialSharedAxis
import com.itsaky.androidide.activities.editor.EditorActivityKt
import com.itsaky.androidide.app.EdgeToEdgeIDEActivity
import com.itsaky.androidide.databinding.ActivityMainBinding
import com.itsaky.androidide.preferences.internal.GeneralPreferences
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.resources.R.string
import com.itsaky.androidide.templates.ITemplateProvider
import com.itsaky.androidide.utils.DialogUtils
import com.itsaky.androidide.utils.flashInfo
import com.itsaky.androidide.viewmodel.MainViewModel
import com.itsaky.androidide.viewmodel.MainViewModel.Companion.SCREEN_MAIN
import com.itsaky.androidide.viewmodel.MainViewModel.Companion.SCREEN_TEMPLATE_DETAILS
import com.itsaky.androidide.viewmodel.MainViewModel.Companion.SCREEN_TEMPLATE_LIST
import java.io.File

class MainActivity : EdgeToEdgeIDEActivity() {

  private val viewModel by viewModels<MainViewModel>()
  private var _binding: ActivityMainBinding? = null

  private val onBackPressedCallback = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      viewModel.apply {

        // Ignore back press if project creating is in progress
        if (creatingProject.value == true) {
          return@apply
        }

        val newScreen = when (currentScreen.value) {
          SCREEN_TEMPLATE_DETAILS -> SCREEN_TEMPLATE_LIST
          SCREEN_TEMPLATE_LIST -> SCREEN_MAIN
          else -> SCREEN_MAIN
        }

        if (currentScreen.value != newScreen) {
          setScreen(newScreen)
        }
      }
    }
  }

  private val binding: ActivityMainBinding
    get() = checkNotNull(_binding)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    openLastProject()

    viewModel.currentScreen.observe(this) { screen ->
      if (screen == -1) {
        return@observe
      }

      onScreenChanged(screen)
      onBackPressedCallback.isEnabled = screen != SCREEN_MAIN
    }

    // Data in a ViewModel is kept between activity rebuilds on
    // configuration changes (i.e. screen rotation)
    // * previous == -1 and current == -1 -> this is an initial instantiation of the activity
    if (viewModel.currentScreen.value == -1 && viewModel.previousScreen == -1) {
      viewModel.setScreen(SCREEN_MAIN)
    } else {
      onScreenChanged(viewModel.currentScreen.value)
    }

    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
  }

  /**
   * Overridden to handle system bar insets for Edge-to-Edge display.
   * This method applies top padding to the main content area (`fragmentContainersParent`)
   * to prevent it from drawing under the status bar, especially when the AppBar is hidden.
   * It also applies left and right padding for devices with display cutouts (notches, etc.).
   *
   * @param insets The system bar insets provided by the system.
   * @author android_zero
   */
  override fun onApplySystemBarInsets(insets: Insets) {
    binding.fragmentContainersParent.updatePadding(
      left = insets.left,
      top = insets.top,
      right = insets.right,
      bottom = insets.bottom
    )
  }

  /**
   * Manages fragment visibility and applies transitions when the screen changes.
   * It now also controls the visibility of the AppBarLayout.
   *
   * @param screen The new screen to display.
   * @author android_zero (Refactored for transition logic and AppBar visibility)
   */
  private fun onScreenChanged(screen: Int?) {

    val previous = viewModel.previousScreen
    if (previous != -1 && screen != null) {
      val setAxisToX = (previous in listOf(SCREEN_TEMPLATE_LIST, SCREEN_TEMPLATE_DETAILS) &&
                       screen in listOf(SCREEN_TEMPLATE_LIST, SCREEN_TEMPLATE_DETAILS))

      val axis = if (setAxisToX) MaterialSharedAxis.X else MaterialSharedAxis.Y
      val isForward = screen > previous

      val transition = MaterialSharedAxis(axis, isForward).apply {
        duration = 350 // A standard Material transition duration
        doOnEnd {
          viewModel.isTransitionInProgress = false
          onBackPressedCallback.isEnabled = viewModel.currentScreen.value != SCREEN_MAIN
        }
      }
     
      viewModel.isTransitionInProgress = true
      // Animate the visibility change of the AppBarLayout along with fragment transitions.
      TransitionManager.beginDelayedTransition(binding.root, transition)
    }
    
    // The AppBar is only visible on the main screen.
    binding.appbar.isVisible = (screen == SCREEN_MAIN)

    val currentFragment = when (screen) {
      SCREEN_MAIN -> binding.main
      SCREEN_TEMPLATE_LIST -> binding.templateList
      SCREEN_TEMPLATE_DETAILS -> binding.templateDetails
      else -> throw IllegalArgumentException("Invalid screen id: '$screen'")
    }
    
    // Set fragment visibility
    arrayOf(binding.main, binding.templateList, binding.templateDetails).forEach {
        it.isVisible = (it == currentFragment)
    }
  }

  override fun bindLayout(): View {
    _binding = ActivityMainBinding.inflate(layoutInflater)
    return binding.root
  }

  private fun openLastProject() {
    binding.root.post { tryOpenLastProject() }
  }

  private fun tryOpenLastProject() {
    if (!GeneralPreferences.autoOpenProjects) {
      return
    }

    val openedProject = GeneralPreferences.lastOpenedProject
    if (GeneralPreferences.NO_OPENED_PROJECT == openedProject || TextUtils.isEmpty(openedProject)) {
      return
    }

    val project = File(openedProject)
    if (!project.exists()) {
      flashInfo(string.msg_opened_project_does_not_exist)
      return
    }

    if (GeneralPreferences.confirmProjectOpen) {
      askProjectOpenPermission(project)
    } else {
      openProject(project)
    }
  }

  private fun askProjectOpenPermission(root: File) {
    DialogUtils.newMaterialDialogBuilder(this)
      .setTitle(string.title_confirm_open_project)
      .setMessage(getString(string.msg_confirm_open_project, root.absolutePath))
      .setCancelable(false)
      .setPositiveButton(string.yes) { _, _ -> openProject(root) }
      .setNegativeButton(string.no, null)
      .show()
  }

  internal fun openProject(root: File) {
    IProjectManager.getInstance().openProject(root)
    startActivity(Intent(this, EditorActivityKt::class.java))
  }

  override fun onDestroy() {
    ITemplateProvider.getInstance().release()
    super.onDestroy()
    _binding = null
  }
}
