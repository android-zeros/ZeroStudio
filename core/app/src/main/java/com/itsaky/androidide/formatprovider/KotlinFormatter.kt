package com.itsaky.androidide.formatprovider

import com.facebook.ktfmt.format.Formatter as Ktfmt
import com.facebook.ktfmt.format.FormattingOptions as KtfmtOptions
import com.facebook.ktfmt.format.ParseError
import java.io.PrintStream

/**
 * Represents formatting options for the Kotlin code formatter.
 * This class acts as a bridge between IDE settings and the underlying ktfmt formatter options.
 *
 * @property indentSize The number of spaces to use for a single indentation level. Defaults to 4.
 * @property maxLineLength The maximum line length before the formatter attempts to wrap. Defaults to 100.
 * @property organizeImports If true, unused imports will be removed and the remaining ones will be sorted. Defaults to true.
 */
data class KotlinFormatOptions(
    val indentSize: Int = 4,
    val maxLineLength: Int = 100,
    val organizeImports: Boolean = true
)

/**
 * A powerful code formatter for Kotlin files that uses the 'ktfmt' library from Facebook.
 * It is designed to produce consistent, idiomatic Kotlin code and handles complex reflowing 
 * and chained calls gracefully.
 *
 * This implementation is resilient to parsing errors, returning the original source code
 * if the formatter encounters syntax that it cannot handle.
 *
 * @param options Configuration for the formatter. See [KotlinFormatOptions] for details.
 * @param errorStream A [PrintStream] to which formatting errors will be logged. Defaults to `System.err`.
 */
class KotlinFormatter(
    private val options: KotlinFormatOptions = KotlinFormatOptions(),
    private val errorStream: PrintStream = System.err
) : CodeFormatter {

    /**
     * Formats the given Kotlin source code string according to the configured options.
     *
     * If the source code contains syntax errors that prevent `ktfmt` from parsing it,
     * this method will catch the exception, log an error message, and return the original,
     * unformatted source string.
     *
     * @param source The Kotlin source code to format.
     * @return The formatted code, or the original source if formatting fails.
     */
    override fun format(source: String): String {
        return try {
            // Map our internal options to ktfmt's options for consistency and abstraction.
            val ktfmtOptions = KtfmtOptions(
                maxWidth = options.maxLineLength,
                blockIndent = options.indentSize,
                continuationIndent = options.indentSize * 2, // A common and readable convention is twice the block indent.
                removeUnusedImports = options.organizeImports
                // 'manageTrailingCommas' is a new experimental flag in some ktfmt versions.
                // We'll leave it as default unless explicitly configured.
            )
            Ktfmt.format(ktfmtOptions, source)
        } catch (e: ParseError) {
            // ktfmt can throw ParseError on syntactically incorrect code, which is expected
            // during live editing. We gracefully return the original source.
            // Logging provides useful debug information without crashing the application.
            errorStream.println("ktfmt formatting failed due to a parsing error. This is often expected for incomplete code. Details: ${e.message}")
            source
        } catch (e: Exception) {
            // Catch any other unexpected exceptions from the formatter.
            errorStream.println("An unexpected error occurred during ktfmt formatting. Returning original source.")
            e.printStackTrace(errorStream)
            source
        }
    }
}
