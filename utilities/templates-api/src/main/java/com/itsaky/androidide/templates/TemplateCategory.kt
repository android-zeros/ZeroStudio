package com.itsaky.androidide.templates

import androidx.annotation.DrawableRes
import com.itsaky.androidide.resources.R

/**
 * Defines the categories for project templates.
 * Each category corresponds to a tab in the template selection UI.
 *
 * @property key A unique identifier for the category.
 * @property title The title for the category to be displayed in the UI.
 * @property icon The optional drawable resource for the category icon.
 *
 * @author android_zero
 * @since 2025-11-17
 *
 * Work Flow:
 * 1. Defines default template categories like Mobile, Wear, Tv, etc.
 * 2. Each category has a unique key, a display title, and an optional icon.
 * 3. These categories are used by TemplateListFragment to create tabs and organize templates.
 *
 * How to use:
 * To add a new category, simply add a new `val` in the companion object:
 * `val MyCategory = TemplateCategory("my_key", "My Custom Category", R.drawable.ic_my_icon)`
 */
data class TemplateCategory(
    val key: String,
    val title: String,
    @DrawableRes val icon: Int? = null
) {
    /**
     * @property Mobile "Phone and Tablet" category.
     * @property Wear "Wear OS" category.
     * @property Tv "Television" category.
     * @property Car "Automotive" category.
     * @property XR "XR" category.
     * @property Generic "Generic" category.
     *
     * Work Flow:
     * 1. Defines default template categories.
     * 2. Each category has a unique key, display title, and an optional icon.
     * 3. These categories will be used to create tabs in the template selection screen.
     *
     */
    companion object {
    
         /** "Phone and Tablet" category. */
        val Mobile = TemplateCategory("mobile", "Phone and Tablet", R.drawable.ic_template_devive_phones_tablets)
      
       /** "Wear OS" category. */
        val Wear = TemplateCategory("wear", "Wear OS", R.drawable.ic_template_devive_smartwatch)
        
        /** "Television" category. */
        val Tv = TemplateCategory("tv", "Television", R.drawable.ic_template_devive_television)
        
        /** "Automotive" category. */
        val Car = TemplateCategory("car", "Automotive", R.drawable.ic_template_devive_automotive_navigation_screen)
        
        /** "XR" category for AR/VR applications. */
        val XR = TemplateCategory("xr", "XR", R.drawable.ic_template_devive_xr)
        
        /** "Generic" category for non-specific or multi-platform templates. */
        val Generic = TemplateCategory("generic", "Generic", R.drawable.ic_template_generic)


        /**
         * Returns a list of all default categories.
         *
         * @return A list of [TemplateCategory].
         */
        fun defaultCategories(): List<TemplateCategory> {
            return listOf(Mobile, Wear, Tv, Car, XR, Generic)
        }
    }
}