package com.github.strogolsky.autoissue.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

@Service(Service.Level.PROJECT)
class TodoUpdaterService(private val project: Project) {

    fun appendKeyToCode(pointer: SmartPsiElementPointer<out PsiElement>, issueKey: String) {
        ApplicationManager.getApplication().invokeLater {

            WriteCommandAction.runWriteCommandAction(project, "Update TODO with Jira Task", "AutoIssue", {

                val element = pointer.element ?: return@runWriteCommandAction
                val documentManager = PsiDocumentManager.getInstance(project)
                val document = documentManager.getDocument(element.containingFile) ?: return@runWriteCommandAction

                val textRange = element.textRange
                val originalText = document.getText(textRange)

                val todoIndex = originalText.indexOf("TODO", ignoreCase = true)
                if (todoIndex == -1) return@runWriteCommandAction

                val prefix = originalText.substring(0, todoIndex)

                val suffix = if (originalText.trimEnd().endsWith("*/")) " */" else ""

                val newText = "${prefix}TODO [$issueKey]$suffix"

                document.replaceString(textRange.startOffset, textRange.endOffset, newText)

                documentManager.commitDocument(document)
            })
        }
    }
}