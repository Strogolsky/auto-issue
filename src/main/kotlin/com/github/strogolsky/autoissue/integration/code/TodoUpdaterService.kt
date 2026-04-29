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

@Service(Service.Level.PROJECT)
class TodoUpdaterService(private val project: Project) {

    suspend fun appendKeyToCode(
        pointer: SmartPsiElementPointer<out PsiElement>,
        issueKey: String,
    ) {
        withContext(Dispatchers.Main) {

            val element = pointer.element
                ?: throw SourceCodeUpdateException("Cannot insert Jira key: the target line was deleted.")

            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(element.containingFile)
                ?: throw SourceCodeUpdateException("Cannot insert Jira key: unable to access the source file.")

            WriteCommandAction.runWriteCommandAction(project, "Update TODO with Jira Task", "AutoIssue", {
                val textRange = element.textRange
                val originalText = document.getText(textRange)

                val todoIndex = originalText.indexOf("TODO", ignoreCase = true)
                if (todoIndex == -1) {
                    throw SourceCodeUpdateException("Cannot insert Jira key: 'TODO' marker not found.")
                }

                val prefix = originalText.substring(0, todoIndex)
                val suffix = if (originalText.trimEnd().endsWith("*/")) " */" else ""
                val newText = "${prefix}TODO [$issueKey]$suffix"

                document.replaceString(textRange.startOffset, textRange.endOffset, newText)

                documentManager.commitDocument(document)
            })
        }
    }
}