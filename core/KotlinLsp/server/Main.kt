package org.javacs.kt

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.eclipse.lsp4j.launch.LSPLauncher
import org.javacs.kt.util.ExitingInputStream
import org.javacs.kt.util.tcpConnectToClient
import org.javacs.kt.util.tcpStartServer

class Args {
    /*
     * The language server can currently be launched in three modes:
     *  - Stdio, in which case no argument should be specified (used by default)
     *  - TCP Server, in which case the client has to connect to the specified tcpServerPort (used by the Docker image)
     *  - TCP Client, in which case the server will connect to the specified tcpClientPort/tcpClientHost (optionally used by VSCode)
     */

    @Parameter(names = ["--tcpServerPort", "-sp"])
    var tcpServerPort: Int? = null
    @Parameter(names = ["--tcpClientPort", "-p"])
    var tcpClientPort: Int? = null
    @Parameter(names = ["--tcpClientHost", "-h"])
    var tcpClientHost: String = "localhost"
}

fun main(argv: Array<String>) {
    // Redirect java.util.logging calls (e.g. from LSP4J)
    LOG.connectJULFrontend()

    val args = Args().also { JCommander.newBuilder().addObject(it).build().parse(*argv) }
    val (inStream, outStream) = args.tcpClientPort?.let {
        // Launch as TCP Client
        LOG.connectStdioBackend()
        tcpConnectToClient(args.tcpClientHost, it)
    } ?: args.tcpServerPort?.let {
        // Launch as TCP Server
        LOG.connectStdioBackend()
        tcpStartServer(it)
    } ?: Pair(System.`in`, System.out)

    val server = KotlinLanguageServer()
    val threads = Executors.newSingleThreadExecutor { Thread(it, "client") }
    val launcher = LSPLauncher.createServerLauncher(server, ExitingInputStream(inStream), outStream, threads) { it }

    server.connect(launcher.remoteProxy)
    launcher.startListening()
}

/**
 * 在给定的输入/输出流上运行 Kotlin 语言服务器。
 * 此函数适用于嵌入式环境，例如 Android Service，因为它不处理命令行参数且不会在流关闭时退出进程。
 *
 * @param inStream 用于从客户端读取数据的输入流。
 * @param outStream 用于向客户端写入数据的输出流。
 * @return 一个 Future, 在服务器停止监听时完成。
 */
fun runServer(inStream: InputStream, outStream: OutputStream): Future<Void> {
    // 确保日志系统已连接
    LOG.connectJULFrontend()

    val server = KotlinLanguageServer()
    val threads = Executors.newSingleThreadExecutor { Thread(it, "client") }

    // 使用 LSPLauncher 启动服务器，但不使用 ExitingInputStream
    val launcher = LSPLauncher.createServerLauncher(server, inStream, outStream, threads) { it }

    server.connect(launcher.remoteProxy)
    return launcher.startListening()
}