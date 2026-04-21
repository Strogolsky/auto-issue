package com.github.strogolsky.autoissue.actions

import com.github.strogolsky.autoissue.orchestrator.IssueCreationOrchestrator
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager

class TodoLineMarkerProvider : LineMarkerProvider {
    companion object {
        private val JIRA_ID_PATTERN = Regex("\\[[A-Z]+-\\d+]")
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiComment) return null
        if (!element.text.contains("TODO", ignoreCase = true)) return null
        if (JIRA_ID_PATTERN.containsMatchIn(element.text)) return null
        if (DumbService.isDumb(element.project)) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.General.Add,
            { "Create JIRA issue" },
            { _, psiElement ->
                val project = psiElement.project
                val commentText = psiElement.text
                val confirmed =
                    Messages.showYesNoDialog(
                        project,
                        commentText.take(300),
                        "Create JIRA Issue?",
                        "Create",
                        "Cancel",
                        Messages.getQuestionIcon(),
                    )
                if (confirmed == Messages.YES) {
                    PsiDocumentManager.getInstance(project).commitAllDocuments()
                    val pointer =
                        SmartPointerManager.getInstance(project)
                            .createSmartPsiElementPointer(psiElement)
                    project.service<IssueCreationOrchestrator>().launch(commentText, pointer)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { "Create JIRA issue" },
        )
    }
}
