package ru.zdevs.intellij.c.project

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import ru.zdevs.intellij.c.Icons

class CFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?) =
        when (file.extension?.lowercase()) {
            "c" -> Icons.C
            "cpp" -> Icons.CPP
            "h" -> Icons.H
            "s" -> Icons.ASM
            "mk" -> Icons.MK
            "in" -> Icons.TEMPLATE
            "cmake" -> Icons.CMAKE
            else -> when (file.name) {
                "Makefile" -> Icons.MAKEFILE
                "CMakeLists.txt" -> Icons.CMAKE
                else -> null
            }
        }
}