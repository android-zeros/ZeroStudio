package ru.zdevs.intellij.c.build.event

import com.intellij.build.events.SuccessResult
import com.intellij.build.events.Warning

class SuccessResultImpl(private val isUpToDate: Boolean = false) : SuccessResult {
    override fun isUpToDate(): Boolean {
        return isUpToDate
    }

    override fun getWarnings(): MutableList<Warning> {
        val list: List<Warning> = emptyList()
        return list.toMutableList()
    }
}