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
import com.intellij.openapi.application.ApplicationManager
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

/**
 * Orchestrates the complete workflow for creating a JIRA issue from IDE.
 *
 * This service manages the end-to-end process:
 * 1. Validates configuration
 * 2. Generates issue candidate using AI
 * 3. Shows user dialog for review/editing
 * 4. Creates issue in JIRA
 * 5. Updates source code with issue key
 * 6. Notifies user of result
 *
 * The orchestrator runs asynchronously using a supervised coroutine scope,
 * ensuring that failures in one step don't crash other background tasks.
 */
@Service(Service.Level.PROJECT)
class IssueCreationOrchestrator(private val project: Project) : Disposable {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jiraApiService by lazy { ApplicationManager.getApplication().service<JiraApiService>() }
    private val healthChecker by lazy { project.service<ConfigHealthChecker>() }

    override fun dispose() {
        coroutineScope.cancel()
        thisLogger().debug("IssueCreationOrchestrator disposed")
    }

    /**
     * Launches the issue creation workflow asynchronously.
     *
     * This method schedules the orchestration to run in the background.
     * The workflow executes in the IO dispatcher to avoid blocking the UI.
     *
     * @param instructionText The user's instruction or issue description
     * @param pointer PSI element pointer to the location in code where the TODO/issue was found
     */
    fun launch(
        instructionText: String,
        pointer: SmartPsiElementPointer<out PsiElement>,
    ) {
        thisLogger().debug("Launching issue creation workflow for: '$instructionText'")
        coroutineScope.launch { orchestrate(instructionText, pointer) }
    }

    /**
     * Executes the complete issue creation orchestration workflow.
     *
     * Steps:
     * 1. Validate configuration health (JIRA, LLM settings)
     * 2. Fetch JIRA configuration and project metadata
     * 3. Generate issue candidate using AI agent with project context
     * 4. Show user dialog for review and optional editing
     * 5. Create the JIRA issue via REST API
     * 6. Update source code with the new issue key
     * 7. Notify user of success
     *
     * Error handling: AutoIssueExceptions are user-facing errors shown in notifications.
     * Unexpected exceptions are logged with full stack trace and shown as generic errors.
     *
     * @param instructionText The issue description from the user
     * @param pointer PSI element pointer to the code location
     */
    private suspend fun orchestrate(
        instructionText: String,
        pointer: SmartPsiElementPointer<out PsiElement>,
    ) {
        thisLogger().debug("Step 1: Validating configuration...")
        // 1. UX Validation: Pre-check configs
        if (!healthChecker.validateAndNotify()) {
            thisLogger().warn("Configuration validation failed. Aborting workflow.")
            return
        }

        try {
            thisLogger().debug("Step 2: Fetching JIRA configuration...")
            // 2. Fetch Config
            val jiraConfig = ApplicationManager.getApplication().service<JiraConfigService>().getEffectiveConfig()
            thisLogger().debug("Using JIRA project: ${jiraConfig.projectKey}")

            thisLogger().debug("Step 3: Fetching JIRA metadata and generating issue with AI...")
            // 3. Fetch Jira metadata and run LLM
            val (metadata, candidate) =
                withBackgroundProgress(project, "AutoIssue: Generating issue…") {
                    val meta = jiraApiService.getMetadata(jiraConfig.projectKey)
                    thisLogger().debug("Fetched metadata: ${meta.issueTypes.size} issue types, ${meta.priorities.size} priorities")

                    val task =
                        project.service<JiraIssueGenerationService>().generate(
                            instruction = "Generate issue for: $instructionText",
                            env = ContextEnvironment(project = project, pointer = pointer),
                        )
                    meta to task
                }

            thisLogger().debug("Step 4: Showing issue edit dialog to user...")
            // 4. User review
            val issueRequest: JiraIssueRequest =
                withContext(Dispatchers.Main) {
                    IssueEditDialog(project, candidate, metadata).showAndGetResult()
                } ?: return // user cancelled

            thisLogger().debug("Step 5: Creating JIRA issue with title: '${issueRequest.title}'...")
            // 5. Create JIRA issue
            val issueKey: String =
                withBackgroundProgress(project, "AutoIssue: Creating JIRA issue…", cancellable = false) {
                    jiraApiService.createIssue(issueRequest)
                }
            thisLogger().info("JIRA issue created successfully: $issueKey")

            thisLogger().debug("Step 6: Updating source code with issue key: $issueKey...")
            // 6. Update source code
            project.service<TodoUpdaterService>().appendKeyToCode(pointer, issueKey)
            thisLogger().debug("Source code updated successfully")

            thisLogger().info("Issue creation workflow completed successfully: $issueKey")
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
