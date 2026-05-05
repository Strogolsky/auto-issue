package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.core.context.components.JiraField
import com.github.strogolsky.autoissue.core.context.components.JiraIssueType
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import io.mockk.every
import io.mockk.spyk
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.swing.JSpinner

class IssueEditDialogTest : BasePlatformTestCase() {

    private lateinit var candidate: JiraIssueCandidate
    private lateinit var metadata: JiraProjectMetadata
    private lateinit var dialog: IssueEditDialog

    override fun setUp() {
        super.setUp()

        // --- PREPARE TEST DATA ---
        candidate = JiraIssueCandidate(
            title = "Test Issue",
            description = "Test Description",
            labels = listOf("bug", "ui")
        )

        metadata = JiraProjectMetadata(
            projectKey = "PROJ",
            projectId = "10000",
            issueTypes = listOf(
                JiraIssueType("1", "Bug", false),
                JiraIssueType("2", "Task", false)
            ),
            priorities = listOf(JiraField("3", "High"), JiraField("4", "Low")),
            components = listOf(JiraField("comp-1", "Backend")),
            assignees = listOf(JiraField("acc-1", "John Doe")),
            labels = listOf("bug", "ui", "feature")
        )

        // Native IntelliJ approach for EDT execution in Kotlin
        ApplicationManager.getApplication().invokeAndWait {
            dialog = spyk(IssueEditDialog(project, candidate, metadata))

            // Safe to mock here because we are mocking our own instance, not a platform singleton
            every { dialog.showAndGet() } returns true
        }
    }

    override fun tearDown() {
        ApplicationManager.getApplication().invokeAndWait {
            dialog.close(0)
        }
        super.tearDown()
    }

    fun test_should_InitializeFieldsWithCandidateData() {
        val titleField = getPrivateField("titleField") as JBTextField
        val descriptionArea = getPrivateField("descriptionArea") as JBTextArea
        val issueTypeCombo = getPrivateField("issueTypeCombo") as ComboBox<*>
        val assigneeCombo = getPrivateField("assigneeCombo") as ComboBox<*>

        assertEquals("Test Issue", titleField.text)
        assertEquals("Test Description", descriptionArea.text)

        assertEquals(2, issueTypeCombo.itemCount)

        assertEquals(2, assigneeCombo.itemCount)
        assertEquals("None", (assigneeCombo.getItemAt(0) as JiraField).name)
    }

    fun test_should_BuildRequest_When_ValidDataEntered() {
        val request = dialog.showAndGetResult()

        assertNotNull(request)
        assertEquals("Test Issue", request!!.title)
        assertEquals("Test Description", request.description)
        assertEquals(listOf("bug", "ui"), request.labels)

        assertEquals("1", request.issueTypeId)
        assertEquals("3", request.priorityId)

        assertNull(request.assigneeAccountId)
        assertNull(request.parentIssueKey)

        val expectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertEquals(expectedDate, request.dueDate)
    }

    fun test_should_ParseComplexLabelsStringCorrectly() {
        val labelsField = getPrivateField("labelsField") as TextFieldWithAutoCompletion<*>

        ApplicationManager.getApplication().invokeAndWait {
            labelsField.text = "urgent, frontend   backend, "
        }

        val request = dialog.showAndGetResult()

        assertNotNull(request)
        assertEquals(listOf("urgent", "frontend", "backend"), request!!.labels)
    }

    fun test_should_FormatDueDateAsIsoDate() {
        val dueDatePicker = getPrivateField("dueDatePicker") as JSpinner

        val testDate = Date.from(LocalDate.of(2026, 5, 15).atStartOfDay(ZoneId.systemDefault()).toInstant())

        ApplicationManager.getApplication().invokeAndWait {
            dueDatePicker.value = testDate
        }

        val request = dialog.showAndGetResult()

        assertNotNull(request)
        assertEquals("2026-05-15", request!!.dueDate)
    }

    fun test_should_ReturnNull_When_DialogCanceled() {
        every { dialog.showAndGet() } returns false

        val request = dialog.showAndGetResult()

        assertNull(request)
    }

    private fun getPrivateField(fieldName: String): Any {
        val field = dialog.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(dialog)
    }
}