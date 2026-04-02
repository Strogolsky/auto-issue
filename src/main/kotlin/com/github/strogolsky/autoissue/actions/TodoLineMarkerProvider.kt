package com.github.strogolsky.autoissue.actions

import com.github.strogolsky.autoissue.tasks.GenerateIssueTask
import com.intellij.psi.PsiComment
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.SmartPointerManager

class TodoLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiComment) return null

        if (!element.text.contains("TODO", ignoreCase = true)) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.General.Add,
            { "Create task" },
            { _, psiElement ->
                val project = psiElement.project
                val commentText = psiElement.text

                PsiDocumentManager.getInstance(project).commitAllDocuments()

                val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiElement)

                GenerateIssueTask(project, commentText, pointer).queue()
            },
            GutterIconRenderer.Alignment.LEFT,
            { "Create task" }
        )
    }
}