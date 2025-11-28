package com.catpuppyapp.puppygit.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DialogTitle(
    text:String,
) {
    ScrollableRow { Text(text) }
}
