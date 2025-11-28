package ru.zdevs.intellij.c.build.parser

import com.intellij.build.events.MessageEvent
import ru.zdevs.intellij.c.build.CBuildContext
import java.io.File

abstract class BaseBuildParser(private val ctx: CBuildContext) {

    abstract fun getFilePath(file:String): File

    fun getBuildId(): Any {
        return ctx.buildId
    }

    fun addKind(kind: MessageEvent.Kind) {
        if (kind == MessageEvent.Kind.ERROR) {
            ctx.errors.getAndAdd(1)
        } else if (kind == MessageEvent.Kind.WARNING) {
            ctx.warnings.getAndAdd(1)
        }
    }
}