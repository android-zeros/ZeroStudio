package ru.zdevs.intellij.c.clangd

import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.util.SystemInfo
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.LanguageServerManager
import ru.zdevs.intellij.c.Utils

class ClangdLanguageServer(project: Project) : ProcessStreamConnectionProvider() {
    init {
        val clangdPath = Utils.findExecutableInPATH(CLANGD_EXEC_NAME)
        if (!clangdPath.isNullOrEmpty()) {
            super.setCommands(listOf(clangdPath, "--clang-tidy", "--background-index", "--enable-config"))
            super.setWorkingDirectory(project.basePath)
        } else {
            NotificationGroupManager.getInstance().getNotificationGroup("C/C++ Clangd LSP").createNotification(
                "C/C++ Сlangd LSP",
                "Clangd not found. Make sure it is installed properly (and `$CLANGD_EXEC_NAME` available in PATH), and restart the IDE.",
                NotificationType.ERROR
            ).notify(project)
            LanguageServerManager.getInstance(project).stop("clangdLanguageServer")
        }
    }

    companion object {
        val CLANGD_EXEC_NAME = if (SystemInfo.isWindows) {
            arrayOf("clangd.exe")
        } else {
            arrayOf("clangd", "clangd-19", "clangd-20", "clangd-21", "clangd-22", "clangd-23")
        }
    }
}
