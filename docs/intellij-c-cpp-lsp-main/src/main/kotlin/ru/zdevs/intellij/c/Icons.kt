package ru.zdevs.intellij.c

import com.intellij.openapi.util.IconLoader

class Icons {
    companion object {
        val CPP = IconLoader.getIcon("icons/cpp.svg", Icons::class.java)
        val C = IconLoader.getIcon("icons/c.svg", Icons::class.java)
        val H = IconLoader.getIcon("icons/h.svg", Icons::class.java)
        val TEMPLATE = IconLoader.getIcon("icons/t.svg", Icons::class.java)
        val ASM = IconLoader.getIcon("icons/asm.svg", Icons::class.java)
        val MAKEFILE = IconLoader.getIcon("icons/m.svg", Icons::class.java)
        val MK = IconLoader.getIcon("icons/m_small.svg", Icons::class.java)
        val CLASS = IconLoader.getIcon("icons/class.svg", Icons::class.java)
        val CMAKE = IconLoader.getIcon("icons/cmake.svg", Icons::class.java)
    }
}