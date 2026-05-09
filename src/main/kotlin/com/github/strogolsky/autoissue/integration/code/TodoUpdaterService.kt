package com.github.strogolsky.autoissue.integration.code

import com.github.strogolsky.autoissue.core.exceptions.SourceCodeUpdateException
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for updating source code with generated JIRA issue keys.
 *
 * After a JIRA issue is created, this service updates the original TODO comment
 * in the source code to include the issue key. This creates a direct link between
 * the code and the JIRA issue.
 *
 * Example transformation:
 * - Before: `// TODO: Fix database connection`
 * - After: `// TODO [PROJ-123]: Fix database connection`
 *
 * The update is done as an IDE write action to ensure proper document synchronization
 * and undo/redo support.
 */
@Service(Service.Level.PROJECT)
class TodoUpdaterService(private val project: Project) {
    /**
     * Appends the JIRA issue key to the TODO comment in source code.
     *
     * This method:
     * 1. Retrieves the element from the smart pointer
     * 2. Finds and accesses the containing file's document
     * 3. Locates the TODO marker in the element's text
     * 4. Updates it to include the issue key in format: `TODO [ISSUE-KEY]`
     * 5. Commits the document changes
     *
     * The operation runs on the main dispatcher to ensure UI thread compatibility.
     *
     * @param pointer Smart pointer to the original TODO element
     * @param issueKey The JIRA issue key to append (e.g., "PROJ-123")
     * @throws SourceCodeUpdateException if element is deleted, file is inaccessible, or TODO marker not found
     */
    suspend fun appendKeyToCode(
        pointer: SmartPsiElementPointer<out PsiElement>,
        issueKey: String,
    ) {
        withContext(Dispatchers.Main) {
            val element =
                pointer.element
                    ?: throw SourceCodeUpdateException("Cannot insert Jira key: the target line was deleted before update.")

            val documentManager = PsiDocumentManager.getInstance(project)
            val document =
                documentManager.getDocument(element.containingFile)
                    ?: throw SourceCodeUpdateException("Cannot insert Jira key: unable to access the source file.")

            WriteCommandAction.runWriteCommandAction(project, "Update TODO with Jira Task", "AutoIssue", {
                val textRange = element.textRange
                val originalText = document.getText(textRange)

                val todoIndex = originalText.indexOf("TODO", ignoreCase = true)
                if (todoIndex == -1) {
                    throw SourceCodeUpdateException("Cannot insert Jira key: 'TODO' marker not found in the target element.")
                }

                // Extract prefix and suffix to preserve formatting
                val prefix = originalText.substring(0, todoIndex)
                val suffix = if (originalText.trimEnd().endsWith("*/")) " */" else ""
                val newText = "${prefix}TODO [$issueKey]$suffix"

                document.replaceString(textRange.startOffset, textRange.endOffset, newText)
                documentManager.commitDocument(document)
            })
        }
    }
}
