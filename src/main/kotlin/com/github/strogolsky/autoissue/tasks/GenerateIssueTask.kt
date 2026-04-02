package com.github.strogolsky.autoissue.tasks

import com.github.strogolsky.autoissue.agent.context.ContextEnvironment
import com.github.strogolsky.autoissue.services.JiraApiService
import com.github.strogolsky.autoissue.services.JiraIssueGenerationService
import com.github.strogolsky.autoissue.services.TodoUpdaterService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import kotlinx.coroutines.runBlocking

class GenerateIssueTask(
    project: Project,
    private val instructionText: String,
    private val pointer: SmartPsiElementPointer<out PsiElement>,
) : Task.Backgroundable(project, "AutoIssue: Processing Issue", true) {
    override fun run(indicator: ProgressIndicator) {
        try {
            indicator.text = "Extracting code context..."
            indicator.isIndeterminate = true

            val generationService = project.service<JiraIssueGenerationService>()
            val jiraApiService = project.service<JiraApiService>()
            val todoUpdater = project.service<TodoUpdaterService>()

            val environment =
                ContextEnvironment(
                    project = project,
                    pointer = pointer,
                )

            indicator.text = "Asking AI to generate issue..."
            val generatedTask =
                runBlocking {
                    generationService.generateTask("Generate issue for: $instructionText", environment)
                }

            indicator.text = "Creating issue in Jira..."
            val issueKey =
                runBlocking {
                    jiraApiService.createIssue(generatedTask)
                }

            indicator.text = "Updating source code..."
            todoUpdater.appendKeyToCode(pointer, issueKey)

            showNotification(
                "Task Created",
                "Successfully created Jira issue: $issueKey",
                NotificationType.INFORMATION,
            )
        } catch (e: Exception) {
            thisLogger().error("Failed to generate/create issue", e)
            showNotification("Error", "Failed: ${e.message}", NotificationType.ERROR)
        }
    }

    private fun showNotification(
        title: String,
        content: String,
        type: NotificationType,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AutoIssue Notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
}
