package ru.zdevs.intellij.c.build

import com.intellij.execution.process.KillableProcessHandler
import java.util.concurrent.atomic.AtomicInteger

class CBuildContext(val processHandler: KillableProcessHandler) {
    val buildId = Any()
    val errors = AtomicInteger()
    val warnings = AtomicInteger()
}