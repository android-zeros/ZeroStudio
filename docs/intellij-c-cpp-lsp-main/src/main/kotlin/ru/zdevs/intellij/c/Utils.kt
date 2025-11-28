package ru.zdevs.intellij.c

import com.intellij.util.EnvironmentUtil
import java.io.File

class Utils {
    companion object {
        fun findExecutableInPATH(executable: Array<String>) =
            EnvironmentUtil.getValue("PATH")?.split(File.pathSeparator)?.firstNotNullOfOrNull { path ->
                executable.firstNotNullOfOrNull { exe -> File(path, exe).takeIf { it.canExecute() } }
            }?.path
    }
}