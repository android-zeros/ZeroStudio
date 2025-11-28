package com.itsaky.androidide.actions.cursor

/**
 * A simple data class to hold information about a parsed symbol.
 */
data class SymbolInfo(
    val name: String,
    val kind: String,
    val line: Int,
    val column: Int
)