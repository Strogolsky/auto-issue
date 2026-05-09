package com.github.strogolsky.autoissue.ui.action

import com.github.strogolsky.autoissue.orchestration.IssueCreationOrchestrator
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

/**
 * Gutter icon provider that displays clickable "Create Issue" icons next to TODO comments.
 *
 * Scans source code comments and displays a "+" icon in the editor gutter for TODO comments
 * that don't already contain a JIRA issue key (e.g., [PROJ-123]). Clicking the icon
 * triggers the issue generation workflow.
 *
 * Uses IntelliJ's LineMarkerProvider API for integration with the code editor.
 * Only activates on actual TODO comments in the source (not DSL or build files).
 */
class TodoLineMarkerProvider : LineMarkerProvider {
    companion object {
        // Pattern to detect JIRA issue keys (e.g., [PROJ-123] or [ABC-456])
        private val JIRA_ID_PATTERN = Regex("\\[[A-Z]+-\\d+]")
    }

    /**
     * Called by the IDE for each PSI element to determine if a line marker should be shown.
     *
     * Filters for TODO comments that don't already have a JIRA issue key attached.
     * Returns a LineMarkerInfo that displays a gutter icon; clicking it shows a confirmation
     * dialog and launches the issue generation workflow.
     *
     * @param element The PSI element to check (could be any code element)
     * @return LineMarkerInfo if this element should display a gutter icon, or null otherwise
     */
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
