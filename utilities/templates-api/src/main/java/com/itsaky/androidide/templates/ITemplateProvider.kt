package com.itsaky.androidide.templates

import com.itsaky.androidide.utils.ServiceLoader

/**
 * An [ITemplateProvider] provides templates to the IDE, organized by categories.
 *
 * @author Akash Yadav
 * @author android_zero (Refactored for categorization)
 */
interface ITemplateProvider {

    companion object {
        private var provider: ITemplateProvider? = null

        /**
         * Get the template provider instance.
         *
         * @param reload Whether to reload the provider. If the value is `true`
         * and the provider is cached, the provider is cleared and loaded again.
         * @return The singleton instance of [ITemplateProvider].
         */
        @JvmStatic
        @JvmOverloads
        fun getInstance(reload: Boolean = false): ITemplateProvider {
            if (provider == null || reload) {
                provider = ServiceLoader.load(ITemplateProvider::class.java).findFirstOrThrow().apply {
                    if (reload) reload()
                }
            }
            return provider!!
        }

        /**
         * @return Whether the [ITemplateProvider] has been loaded or not.
         */
        @JvmStatic
        fun isLoaded() = provider != null
    }

    /**
     * Registers a template under a specific category. This allows for dynamic addition of templates.
     *
     * @param category The [TemplateCategory] under which to register the template.
     * @param template The [Template] to register.
     *
     * Usage:
     * ```kotlin
     * val myTemplate = ... // create a template instance
     * ITemplateProvider.getInstance().registerTemplate(TemplateCategory.Mobile, myTemplate)
     * ```
     */
    fun registerTemplate(category: TemplateCategory, template: Template<*>)

    /**
     * Get all registered template categories.
     *
     * @return A list of [TemplateCategory]s that have at least one template registered.
     */
    fun getRegisteredCategories(): List<TemplateCategory>

    /**
     * Get all templates for a specific category.
     *
     * @param category The [TemplateCategory] to get templates for.
     * @return A list of [Template]s for the given category. Returns an empty list if the category is not found or has no templates.
     */
    fun getTemplatesFor(category: TemplateCategory): List<Template<*>>

    /**
     * Get a specific template by its unique ID. It will search across all categories.
     *
     * @param templateId The ID for the template.
     * @return The [Template] with the given [templateId] if any, or `null`.
     */
    fun getTemplate(templateId: String): Template<*>?

    /**
     * Reloads all templates from their sources.
     */
    fun reload()

    /**
     * Clears all registered templates and categories from the provider.
     */
    fun release()
}