package com.itsaky.androidide.tooling.api.messages

import java.io.Serializable

/**
 * 封装了从客户端发送到服务端的测试执行请求。
 * @author android_zero
 */
data class TestExecutionMessage(
    /**
     * 要执行的目标任务的路径列表，例如 `listOf(":app:testDebugUnitTest")`。
     * 如果为空，Gradle 将尝试在所有项目中运行匹配的测试。
     */
    val tasks: List<String>,

    /**
     * 要执行的特定 JVM 测试的列表。
     * 允许按类或按方法指定测试。
     */
    val jvmTests: List<JvmTestSpec>,

    /**
     * 如果为 true，则以调试模式运行测试，并在指定的端口上等待调试器连接。
     * 值为 0 或负数表示禁用调试模式。
     */
    val debugPort: Int = 0
) : Serializable

/**
 * 定义一个要执行的 JVM 测试规范。
 * @author android_zero
 *
 * @property className 要运行的测试类的完全限定名。
 * @property methodName (可选) 如果指定，则只运行该类中的这个方法。如果为 null，则运行该类中的所有测试方法。
 */
data class JvmTestSpec(
    val className: String,
    val methodName: String? = null
) : Serializable