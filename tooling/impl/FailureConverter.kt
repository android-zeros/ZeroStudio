package com.itsaky.androidide.tooling.impl.util

import com.itsaky.androidide.tooling.api.messages.result.Failure
import com.itsaky.androidide.tooling.impl.progress.EventTransformer
import org.gradle.tooling.FileComparisonTestAssertionFailure
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.TestAssertionFailure
import org.gradle.tooling.TestFailure
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 工具类，用于将 Gradle Tooling API 的异常和 Failure 对象
 * 转换为可序列化的自定义 Failure DTO。
 * @author android_zero
 */
object FailureConverter {

    /**
     * 将任意 Throwable 转换为自定义的 Failure DTO。
     * 这是转换逻辑的主入口点。
     */
    fun createFailureModel(error: Throwable): Failure {
        return if (error is GradleConnectionException) {
            val gradleFailures = try {
                error.failures
            } catch (e: NoSuchMethodError) {
                null
            }

            if (!gradleFailures.isNullOrEmpty()) {
                val causes = gradleFailures.map { convertGradleFailure(it) }
                Failure(
                    message = error.message,
                    description = getStackTraceAsString(error),
                    causes = causes
                )
            } else {
                convertThrowable(error)
            }
        } else {
            convertThrowable(error)
        }
    }

    /**
     * 递归地将 Throwable 转换为自定义的 Failure DTO。
     */
    private fun convertThrowable(t: Throwable?): Failure {
        if (t == null) {
            return Failure(null, null)
        }
        val causes = if (t.cause != null && t.cause != t) {
            listOf(convertThrowable(t.cause))
        } else {
            emptyList()
        }
        return Failure(
            message = t.message,
            description = getStackTraceAsString(t),
            causes = causes
        )
    }

    /**
     * 递归地将 org.gradle.tooling.Failure 转换为自定义的 Failure DTO。
     */
    private fun convertGradleFailure(failure: org.gradle.tooling.Failure): Failure {
        val causes = failure.causes.map { convertGradleFailure(it) }
        val problems = try {
            failure.problems.map { EventTransformer.problemEvent(it).problem }
        } catch (e: NoSuchMethodError) {
            emptyList()
        }
        
        return when (failure) {
            is FileComparisonTestAssertionFailure -> Failure(
                message = failure.message,
                description = failure.description,
                causes = causes,
                problems = problems,
                className = failure.className,
                stacktrace = failure.stacktrace,
                expected = failure.expected,
                actual = failure.actual,
                expectedContent = failure.expectedContent,
                actualContent = failure.actualContent
            )
            is TestAssertionFailure -> Failure(
                message = failure.message,
                description = failure.description,
                causes = causes,
                problems = problems,
                className = failure.className,
                stacktrace = failure.stacktrace,
                expected = failure.expected,
                actual = failure.actual
            )
            is TestFailure -> Failure(
                message = failure.message,
                description = failure.description,
                causes = causes,
                problems = problems,
                className = failure.className,
                stacktrace = failure.stacktrace
            )
            else -> Failure(
                message = failure.message,
                description = failure.description,
                causes = causes,
                problems = problems
            )
        }
    }

    private fun getStackTraceAsString(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }
}