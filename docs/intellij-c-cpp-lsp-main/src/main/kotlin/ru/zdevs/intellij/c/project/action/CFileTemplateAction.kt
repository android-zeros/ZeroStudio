package ru.zdevs.intellij.c.project.action

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import ru.zdevs.intellij.c.Icons

class CFileTemplateAction : CreateFileFromTemplateAction("C/C++ File", "Create new C/C++ File", Icons.C) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder.setTitle("New C/C++ File")
            .addKind("C Source", Icons.C, "source.c")
            .addKind("C++ Source", Icons.CPP, "source.cpp")
            .addKind("C/C++ Header", Icons.H, "header.h")
            .addKind("C++ Class", Icons.CLASS, "class.h")
    }

    override fun createFileFromTemplate(name : String, template : FileTemplate, dir : PsiDirectory) : PsiFile {
        val file = super.createFileFromTemplate(name, template, dir)
        if (template.name == "class") {
            val templateCpp = FileTemplateManager.getInstance(dir.project).getInternalTemplate("class.cpp")
            super.createFileFromTemplate(name, templateCpp, dir)
        }
        return file
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return "C/C++ File"
    }
}