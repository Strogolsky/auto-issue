package com.github.strogolsky.autoissue.orchestration

import com.github.strogolsky.autoissue.core.JiraIssueGenerationService
import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.exceptions.AutoIssueException
import com.github.strogolsky.autoissue.core.output.JiraIssueRequest
import com.github.strogolsky.autoissue.integration.code.TodoUpdaterService
import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.validation.ConfigHealthChecker
import com.github.strogolsky.autoissue.ui.components.IssueEditDialog
import com.github.strogolsky.autoissue.ui.notifications.AutoIssueNotifier
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
    private val healthChecker by lazy { project.service<ConfigHealthChecker>() }

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
        // 1. UX Validation: Pre-check configs
        if (!healthChecker.validateAndNotify()) return

        try {
            // 2. Fetch Config
            val jiraConfig = project.service<JiraConfigService>().getEffectiveConfig()

            // 3. Fetch Jira metadata and run LLM
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

            // 4. User review
            val issueRequest: JiraIssueRequest =
                withContext(Dispatchers.Main) {
                    IssueEditDialog(project, candidate, metadata).showAndGetResult()
                } ?: return // user cancelled

            // 5. Create JIRA issue
            val issueKey: String =
                withBackgroundProgress(project, "AutoIssue: Creating JIRA issue…", cancellable = false) {
                    jiraApiService.createIssue(issueRequest)
                }

            // 6. Update source code
            project.service<TodoUpdaterService>().appendKeyToCode(pointer, issueKey)

            AutoIssueNotifier.notify(project, "Successfully created JIRA issue: $issueKey", NotificationType.INFORMATION)
        } catch (e: AutoIssueException) {
            thisLogger().warn("Operation interrupted: ${e.message}", e)
            AutoIssueNotifier.notify(project, e.message ?: "An operation failed", NotificationType.ERROR)
        } catch (e: Exception) {
            thisLogger().error("Unexpected system error during issue creation", e)
            AutoIssueNotifier.notify(project, "Unexpected system error: ${e.message}", NotificationType.ERROR)
        }
    }
}
