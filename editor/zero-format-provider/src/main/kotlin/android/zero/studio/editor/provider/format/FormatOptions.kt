package com.itsaky.androidide.formatprovider


/*
*用于定义格式化的风格样式，缩进距离等
* @android_zero
*/

data class FormatOptions(
    val indentSize: Int = 4,
    val useTabs: Boolean = false
)

data class JavaFormatOptions(
    val style: Style = Style.AOSP,
    val organizeImports: Boolean = true
) {
    enum class Style { AOSP, GOOGLE }
}

// NOTE: This class was redeclared in another file.
// The other declaration in 'KotlinFormatOptions.kt' is kept.
//
// data class KotlinFormatOptions(
//    val indentSize: Int = 4,
//    val maxLineLength: Int = 120,
//    val organizeImports: Boolean = true,
//    val useAndroidKdocStyle: Boolean = true
// )