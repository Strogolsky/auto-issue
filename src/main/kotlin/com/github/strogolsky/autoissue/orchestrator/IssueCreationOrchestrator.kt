package com.github.strogolsky.autoissue.orchestrator

import com.github.strogolsky.autoissue.agent.context.ContextEnvironment
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.github.strogolsky.autoissue.services.JiraApiService
import com.github.strogolsky.autoissue.services.JiraConfigService
import com.github.strogolsky.autoissue.services.JiraIssueGenerationService
import com.github.strogolsky.autoissue.services.TodoUpdaterService
import com.github.strogolsky.autoissue.settings.AgentConfigService
import com.github.strogolsky.autoissue.ui.TicketEditDialog
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
    private val cs = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
            val agentConfig =
                project.service<AgentConfigService>().getEffectiveConfig()
                    ?: return notify("LLM configuration incomplete. Check plugin settings.", NotificationType.ERROR)
            try {
                project.service<JiraConfigService>().getEffectiveConfig()
            } catch (e: IllegalArgumentException) {
                return notify("JIRA configuration incomplete: ${e.message}", NotificationType.ERROR)
            }

            // 2. Gather context and run LLM agent
            val candidate: JiraTaskCandidate =
                withBackgroundProgress(project, "AutoIssue: Generating issue…") {
                    project.service<JiraIssueGenerationService>().generateTask(
                        instruction = "Generate issue for: $instructionText",
                        env = ContextEnvironment(project = project, pointer = pointer),
                    )
                }

            // 3. Let the user review and edit the candidate before creating
            val editedCandidate: JiraTaskCandidate =
                withContext(Dispatchers.Main) {
                    TicketEditDialog(project, candidate).showAndGetResult()
                } ?: return // user cancelled

            // 4. Create the JIRA issue — non-cancellable once started
            val issueKey: String =
                withBackgroundProgress(project, "AutoIssue: Creating JIRA issue…", cancellable = false) {
                    project.service<JiraApiService>().createIssue(editedCandidate)
                }

            // 5. Update source code: TODO → TODO [PROJ-42]
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
