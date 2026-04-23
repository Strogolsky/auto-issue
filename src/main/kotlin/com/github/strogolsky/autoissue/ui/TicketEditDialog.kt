package com.github.strogolsky.autoissue.ui

import com.github.strogolsky.autoissue.agent.context.components.JiraField
import com.github.strogolsky.autoissue.agent.context.components.JiraIssueType
import com.github.strogolsky.autoissue.agent.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.agent.output.JiraIssueRequest
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import javax.swing.JComponent

class TicketEditDialog(
    project: Project,
    private val candidate: JiraTaskCandidate,
    private val metadata: JiraProjectMetadata,
) : DialogWrapper(project) {
    private val titleField = JBTextField(candidate.title)
    private val descriptionArea =
        JBTextArea(candidate.description).apply {
            rows = 8
            lineWrap = true
            wrapStyleWord = true
        }
    private val issueTypeCombo =
        ComboBox(metadata.issueTypes.toTypedArray()).apply {
            renderer = SimpleListCellRenderer.create("") { it?.name ?: "" }
        }
    private val priorityCombo =
        ComboBox(metadata.priorities.toTypedArray()).apply {
            renderer = SimpleListCellRenderer.create("") { it?.name ?: "" }
        }
    private val assigneeCombo =
        ComboBox((listOf(noneAssignee) + metadata.assignees).toTypedArray()).apply {
            renderer = SimpleListCellRenderer.create("") { it?.name ?: "None" }
        }
    private val labelsField = JBTextField(candidate.labels.joinToString(", "))
    private val parentField = JBTextField().apply { emptyText.text = "e.g. PROJ-123 (optional)" }
    private val dueDatePicker = DatePickerField()

    init {
        title = "Review JIRA Issue"
        setOKButtonText("Create Issue")
        init()
    }

    override fun createCenterPanel(): JComponent =
        panel {
            row("Title:") { cell(titleField).align(Align.FILL) }
            row("Description:") { cell(JBScrollPane(descriptionArea)).align(Align.FILL) }
            row("Issue Type:") { cell(issueTypeCombo) }
            row("Priority:") { cell(priorityCombo) }
            row("Assignee:") { cell(assigneeCombo) }
            row("Labels:") { cell(labelsField).align(Align.FILL) }
            row("Parent:") { cell(parentField).align(Align.FILL) }
            row("Due Date:") { cell(dueDatePicker) }
        }.also { it.preferredSize = Dimension(640, 480) }

    fun showAndGetResult(): JiraIssueRequest? {
        if (!showAndGet()) return null
        return JiraIssueRequest(
            title = titleField.text.trim(),
            description = descriptionArea.text.trim(),
            labels = labelsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            issueTypeId = (issueTypeCombo.selectedItem as? JiraIssueType)?.id ?: return null,
            priorityId = (priorityCombo.selectedItem as? JiraField)?.id ?: return null,
            assigneeAccountId = (assigneeCombo.selectedItem as? JiraField)?.takeIf { it.id.isNotEmpty() }?.id,
            parentIssueKey = parentField.text.trim().takeIf { it.isNotEmpty() },
            dueDate = dueDatePicker.selectedDate?.toString(),
        )
    }

    companion object {
        private val noneAssignee = JiraField("", "None")
    }
}
