package com.itsaky.androidide.actions

import android.graphics.Typeface

/**
 * 一个通用的样式约束类，用于定义Action按钮在UI中的外观。
 *
 * 通过在 [BaseEditorAction] 子类中设置此对象，可以自定义特定Action的显示样式。
 * UI渲染逻辑会读取这些属性并应用到对应的View上。
 *
 * @author: android_zero
 *
 * @property textSizeSp 字体大小，单位为 sp。如果为 null，则使用默认大小。
 * @property paddingHorizontalDp 水平内边距，单位为 dp。如果为 null，则使用默认内边距。
 * @property minWidthDp 最小宽度，单位为 dp。如果为 null，则不设置最小宽度。
 * @property typefaceStyle 字体样式，例如 Typeface.NORMAL, Typeface.BOLD。
 */
data class ActionStyle(
    val textSizeSp: Float? = null,
    val paddingHorizontalDp: Int? = null,
    val minWidthDp: Int? = null,
    val typefaceStyle: Int? = null
)