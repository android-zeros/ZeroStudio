package com.itsaky.androidide.tooling.api.messages.result

import com.itsaky.androidide.tooling.events.problems.Problem
import java.io.Serializable

/**
 * 一个可序列化的数据传输对象（DTO），用于封装详细的构建失败信息。
 * 它镜像了 `org.gradle.tooling.Failure` 的结构，并支持递归的失败原因链。
 * @author android_zero
 */
data class Failure(
    /**
     * 失败的简短消息。
     */
    val message: String?,

    /**
     * 失败的详细描述，通常包含堆栈跟踪。
     */
    val description: String?,

    /**
     * 导致此失败的原因列表，每个原因本身也是一个 `Failure` 对象。
     */
    val causes: List<Failure> = emptyList(),
    
    /**
     * 与此失败相关的结构化问题列表。
     * (需要 Gradle 8.12+)
     */
    val problems: List<Problem> = emptyList(),


    /**
     * 对于测试失败，这是测试类的完全限定名。
     */
    val className: String? = null,

    /**
     * 对于测试失败，这是完整的堆栈跟踪字符串。
     */
    val stacktrace: String? = null,

    /**
     * 对于断言失败，这是期望值的字符串表示。
     */
    val expected: String? = null,

    /**
     * 对于断言失败，这是实际值的字符串表示。
     */
    val actual: String? = null,

    /**
     * 对于文件比较断言失败，这是期望文件的字节内容 (Base64 编码)。
     */
    val expectedContent: ByteArray? = null,

    /**
     * 对于文件比较断言失败，这是实际文件的字节内容 (Base64 编码)。
     */
    val actualContent: ByteArray? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Failure

        if (message != other.message) return false
        if (description != other.description) return false
        if (causes != other.causes) return false
        if (problems != other.problems) return false
        if (className != other.className) return false
        if (stacktrace != other.stacktrace) return false
        if (expected != other.expected) return false
        if (actual != other.actual) return false
        if (expectedContent != null) {
            if (other.expectedContent == null) return false
            if (!expectedContent.contentEquals(other.expectedContent)) return false
        } else if (other.expectedContent != null) return false
        if (actualContent != null) {
            if (other.actualContent == null) return false
            if (!actualContent.contentEquals(other.actualContent)) return false
        } else if (other.actualContent != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message?.hashCode() ?: 0
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + causes.hashCode()
        result = 31 * result + problems.hashCode()
        result = 31 * result + (className?.hashCode() ?: 0)
        result = 31 * result + (stacktrace?.hashCode() ?: 0)
        result = 31 * result + (expected?.hashCode() ?: 0)
        result = 31 * result + (actual?.hashCode() ?: 0)
        result = 31 * result + (expectedContent?.contentHashCode() ?: 0)
        result = 31 * result + (actualContent?.contentHashCode() ?: 0)
        return result
    }
}