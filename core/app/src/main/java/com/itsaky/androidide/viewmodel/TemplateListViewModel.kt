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
 *
 * @author android_zero
 */
package com.itsaky.androidide.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itsaky.androidide.templates.ITemplateProvider
import com.itsaky.androidide.templates.Template
import com.itsaky.androidide.templates.TemplateCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Defines the grid layout styles for the template list.
 *
 * @param columns The number of columns in the grid.
 */
enum class GridLayoutType(val columns: Int) {
    /** Represents a grid with 2 columns, suitable for a "four-grid" like layout. */
    FOUR_GRID(2),
    /** Represents a grid with 3 columns, suitable for a "six-grid" like layout. */
    SIX_GRID(3)
}

/**
 * ViewModel for [com.itsaky.androidide.fragments.TemplateListFragment].
 * Manages the state for the template selection UI, including categories, templates,
 * and layout configuration.
 *
 * Work Flow:
 * 1. On initialization, it fetches all registered categories and their templates from [ITemplateProvider].
 * 2. It exposes these as `StateFlow`s for the Compose UI to observe.
 * 3. It holds the current grid layout style, which can be configured programmatically.
 *
 * @property _categories A mutable state flow for the list of template categories.
 * @property categories An immutable state flow exposing the categories to the UI.
 * @property _templatesByCategory A mutable state flow for the map of templates grouped by category.
 * @property templatesByCategory An immutable state flow exposing the categorized templates.
 * @property _gridLayoutType A mutable state flow for the current grid layout style.
 * @property gridLayoutType An immutable state flow for the grid layout style.
 */
class TemplateListViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<TemplateCategory>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _templatesByCategory = MutableStateFlow<Map<TemplateCategory, List<Template<*>>>>(emptyMap())
    val templatesByCategory = _templatesByCategory.asStateFlow()

    // Default layout is a 2-column grid. This can be changed programmatically.
    private val _gridLayoutType = MutableStateFlow(GridLayoutType.FOUR_GRID)
    val gridLayoutType = _gridLayoutType.asStateFlow()

    init {
        loadTemplates()
    }
    
    /**
     * Programmatically sets the grid layout type.
     * @param gridType The desired [GridLayoutType].
     */
    fun setGridLayoutType(gridType: GridLayoutType) {
        _gridLayoutType.value = gridType
    }

    /**
     * Loads categories and templates from the [ITemplateProvider] on a background thread.
     * Updates the StateFlows, which will trigger a recomposition in the UI.
     */
    fun loadTemplates() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val provider = ITemplateProvider.getInstance(reload = true)
                val categories = provider.getRegisteredCategories()
                val templatesMap = categories.associateWith { provider.getTemplatesFor(it) }

                withContext(Dispatchers.Main) {
                    _categories.value = categories
                    _templatesByCategory.value = templatesMap
                }
            }
        }
    }
}