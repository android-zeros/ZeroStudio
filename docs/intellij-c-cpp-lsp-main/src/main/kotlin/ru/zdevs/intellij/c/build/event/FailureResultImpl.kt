package ru.zdevs.intellij.c.build.event

import com.intellij.build.events.Failure
import com.intellij.build.events.FailureResult
import com.intellij.build.events.impl.FailureImpl

class FailureResultImpl(errorCode: Int, error: Throwable?) : FailureResult {
    private val failures: MutableList<Failure> = ArrayList()

    init {
        if (error != null)
            failures.add(FailureImpl(null, error))
        else
            failures.add(FailureImpl("Build failed", "Error code $errorCode"))
    }

    override fun getFailures(): MutableList<Failure> {
        return failures
    }
}
