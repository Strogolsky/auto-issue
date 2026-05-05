package com.github.strogolsky.autoissue.orchestration

import com.github.strogolsky.autoissue.core.JiraIssueGenerationService
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.core.exceptions.AutoIssueException
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.core.output.JiraIssueRequest
import com.github.strogolsky.autoissue.integration.code.TodoUpdaterService
import com.github.strogolsky.autoissue.integration.jira.JiraApiService
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.validation.ConfigHealthChecker
import com.github.strogolsky.autoissue.ui.components.IssueEditDialog
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.util.ui.UIUtil
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

class IssueCreationOrchestratorTest : BasePlatformTestCase() {

    private lateinit var orchestrator: IssueCreationOrchestrator

    private lateinit var mockHealthChecker: ConfigHealthChecker
    private lateinit var mockJiraConfigService: JiraConfigService
    private lateinit var mockJiraApiService: JiraApiService
    private lateinit var mockIssueGenerationService: JiraIssueGenerationService
    private lateinit var mockTodoUpdaterService: TodoUpdaterService
    private lateinit var mockPointer: SmartPsiElementPointer<PsiElement>

    private val capturedNotifications = mutableListOf<Notification>()

    override fun setUp() {
        super.setUp()
        capturedNotifications.clear()

        mockHealthChecker = mockk(relaxed = true)
        mockJiraConfigService = mockk(relaxed = true)
        mockJiraApiService = mockk(relaxed = true)
        mockIssueGenerationService = mockk(relaxed = true)
        mockTodoUpdaterService = mockk(relaxed = true)
        mockPointer = mockk(relaxed = true)

        project.replaceService(ConfigHealthChecker::class.java, mockHealthChecker, testRootDisposable)
        project.replaceService(JiraIssueGenerationService::class.java, mockIssueGenerationService, testRootDisposable)
        project.replaceService(TodoUpdaterService::class.java, mockTodoUpdaterService, testRootDisposable)

        ApplicationManager.getApplication().replaceService(JiraApiService::class.java, mockJiraApiService, testRootDisposable)
        ApplicationManager.getApplication().replaceService(JiraConfigService::class.java, mockJiraConfigService, testRootDisposable)

        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    capturedNotifications.add(notification)
                }
            }
        )

        mockkConstructor(IssueEditDialog::class)

        orchestrator = IssueCreationOrchestrator(project)
    }

    override fun tearDown() {
        unmockkAll()

        orchestrator.dispose()
        super.tearDown()
    }

    fun test_should_AbortPipeline_When_ConfigIsInvalid() {
        // --- PREPARE ---
        every { mockHealthChecker.validateAndNotify() } returns false

        // --- ACT ---
        orchestrator.launch("Fix bug", mockPointer)

        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 300) {
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }

        // --- ASSERT ---
        coVerify(exactly = 0) { mockJiraApiService.getMetadata(any()) }
    }

    fun test_should_CompletePipelineSuccessfully() {
        // --- PREPARE ---
        every { mockHealthChecker.validateAndNotify() } returns true
        coEvery { mockJiraApiService.getMetadata(any()) } returns mockk<JiraProjectMetadata>(relaxed = true)
        coEvery { mockIssueGenerationService.generateTask(any(), any()) } returns mockk<JiraIssueCandidate>(relaxed = true)
        every { anyConstructed<IssueEditDialog>().showAndGetResult() } returns mockk<JiraIssueRequest>(relaxed = true)
        coEvery { mockJiraApiService.createIssue(any()) } returns "PROJ-123"


        // --- ACT ---
        orchestrator.launch("Implement auth", mockPointer)

        val start = System.currentTimeMillis()
        while (capturedNotifications.isEmpty() && System.currentTimeMillis() - start < 3000) {
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }

        // --- ASSERT ---
        assertTrue("Success notification was not fired", capturedNotifications.isNotEmpty())
        val notification = capturedNotifications.first()
        assertEquals(NotificationType.INFORMATION, notification.type)
        assertTrue(notification.content.contains("PROJ-123"))
    }

    fun test_should_AbortPipeline_When_UserCancelsDialog() {
        // --- PREPARE ---
        every { mockHealthChecker.validateAndNotify() } returns true
        coEvery { mockJiraApiService.getMetadata(any()) } returns mockk(relaxed = true)
        coEvery { mockIssueGenerationService.generateTask(any(), any()) } returns mockk(relaxed = true)

        var didReachDialog = false
        every { anyConstructed<IssueEditDialog>().showAndGetResult() } answers {
            didReachDialog = true
            null
        }

        // --- ACT ---
        orchestrator.launch("Fix styling", mockPointer)

        val start = System.currentTimeMillis()
        while (!didReachDialog && System.currentTimeMillis() - start < 3000) {
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }

        Thread.sleep(100)

        // --- ASSERT ---
        coVerify(exactly = 0) { mockJiraApiService.createIssue(any()) }
    }

    fun test_should_ShowErrorNotification_When_BusinessExceptionThrown() {
        // --- PREPARE ---
        every { mockHealthChecker.validateAndNotify() } returns true

        coEvery {
            mockIssueGenerationService.generateTask(any(), any())
        } throws object : AutoIssueException("LLM Agent failed to respond") {}

        // --- ACT ---
        orchestrator.launch("Do something", mockPointer)

        val start = System.currentTimeMillis()
        while (capturedNotifications.isEmpty() && System.currentTimeMillis() - start < 3000) {
            UIUtil.dispatchAllInvocationEvents()
            Thread.sleep(10)
        }

        // --- ASSERT ---
        assertTrue("Error notification was not fired", capturedNotifications.isNotEmpty())
        val notification = capturedNotifications.first()
        assertEquals(NotificationType.ERROR, notification.type)
        assertTrue(notification.content.contains("LLM Agent failed to respond"))
    }

    fun test_should_CancelCoroutines_When_Disposed() {
        val scopeField = orchestrator.javaClass.getDeclaredField("cs")
        scopeField.isAccessible = true
        val scope = scopeField.get(orchestrator) as CoroutineScope

        assertTrue("Scope should be active initially", scope.isActive)

        orchestrator.dispose()

        assertFalse("Scope should be cancelled after dispose()", scope.isActive)
    }
}