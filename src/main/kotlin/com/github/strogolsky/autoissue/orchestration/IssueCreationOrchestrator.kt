package com.github.strogolsky.autoissue.orchestration

import com.github.strogolsky.autoissue.core.JiraIssueGenerationService
import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.output.JiraIssueRequest
import com.github.strogolsky.autoissue.integration.code.TodoUpdaterService
import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfigService
import com.github.strogolsky.autoissue.ui.components.IssueEditDialog
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class IssueCreationOrchestrator(private val project: Project) : Disposable {
    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jiraApiService by lazy { project.service<JiraApiService>() }

    override fun dispose() = cs.cancel()

    fun launch(
        instructionText: String,
        pointer: SmartPsiElementPointer<out PsiElement>,
    ) {
        cs.launch { orchestrate(instructionText, pointer) }
    }

    private suspend fun orchestrate(
        instructionText: String,
        pointer: SmartPsiElementPointer<out PsiElement>,
    ) {
        try {
            // 1. Validate configuration — fast fail before any network/LLM calls
            try {
                project.service<LlmAgentConfigService>().getEffectiveConfig()
            } catch (e: IllegalArgumentException) {
                return notify("LLM configuration incomplete: ${e.message}", NotificationType.ERROR)
            }

            val jiraConfig =
                try {
                    project.service<JiraConfigService>().getEffectiveConfig()
                } catch (e: IllegalArgumentException) {
                    return notify("JIRA configuration incomplete: ${e.message}", NotificationType.ERROR)
                }

            // 2. Fetch Jira metadata and run LLM agent in parallel background
            val (metadata, candidate) =
                withBackgroundProgress(project, "AutoIssue: Generating issue…") {
                    val meta = jiraApiService.getMetadata(jiraConfig.projectKey)
                    val task =
                        project.service<JiraIssueGenerationService>().generateTask(
                            instruction = "Generate issue for: $instructionText",
                            env = ContextEnvironment(project = project, pointer = pointer),
                        )
                    meta to task
                }

            // 3. Let the user review and edit the candidate before creating
            val issueRequest: JiraIssueRequest =
                withContext(Dispatchers.Main) {
                    IssueEditDialog(project, candidate, metadata).showAndGetResult()
                } ?: return // user cancelled

            // 4. Create the JIRA issue — non-cancellable once started
            val issueKey: String =
                withBackgroundProgress(project, "AutoIssue: Creating JIRA issue…", cancellable = false) {
                    jiraApiService.createIssue(issueRequest)
                }

            // 5. Update source code
            project.service<TodoUpdaterService>().appendKeyToCode(pointer, issueKey)

            notify("Successfully created JIRA issue: $issueKey", NotificationType.INFORMATION)
        } catch (e: Exception) {
            thisLogger().error("Failed to create JIRA issue", e)
            notify("Error: ${e.message}", NotificationType.ERROR)
        }
    }

    private fun notify(
        content: String,
        type: NotificationType,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AutoIssue Notifications")
            .createNotification("AutoIssue", content, type)
            .notify(project)
    }
}
