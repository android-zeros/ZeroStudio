package com.itsaky.androidide.projects.api

import java.io.File

/**
 * NOTE: This is an inferred interface based on the structure of `AndroidModule` and `JavaModule`.
 * It represents the essential contract a project module must fulfill to provide
 * information for the Kotlin compiler environment. Any class like `ModuleProject` can implement this.
 */
interface KotlinModule {
    /** The name of the module (e.g., "app"). */
    fun getName(): String

    /** The root directory of the module. Used as a unique key for caching. */
    fun getModulePath(): File

    /** A collection of all library JARs (dependencies). */
    fun getLibraries(): Collection<File>

    /** The path to the `android.jar` for this module's compile SDK version. Null for non-Android modules. */
    fun getAndroidJar(): File?

    /** A map of all Java source files in the module. */
    fun getJavaFiles(): Map<String, File>
}