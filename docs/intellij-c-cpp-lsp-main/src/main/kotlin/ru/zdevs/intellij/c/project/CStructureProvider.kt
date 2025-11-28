package ru.zdevs.intellij.c.project

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.BasePsiNode
import com.intellij.ide.util.treeView.AbstractTreeNode


class CStructureProvider : TreeStructureProvider {
    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        val nodes = ArrayList<AbstractTreeNode<*>>()
        for (child in children) {
            if (child is BasePsiNode) {
                val file = child.virtualFile
                if (file != null && (file.name.startsWith(".") || HIDDEN_EXTENSION.contains(file.extension))) {
                    continue
                }
            }
            nodes.add(child)
        }
        return nodes
    }

    companion object {
        val HIDDEN_EXTENSION = arrayOf("iml")
    }
}