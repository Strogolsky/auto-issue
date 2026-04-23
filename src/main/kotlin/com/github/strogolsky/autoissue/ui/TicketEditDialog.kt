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
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import javax.swing.JComponent

class TicketEditDialog(
    project: Project,
    private val candidate: JiraTaskCandidate,
    private val metadata: JiraProjectMetadata,
) : DialogWrapper(project) {
    private val titleField = JBTextField(candidate.title)
    private val descriptionArea =
        JBTextArea(candidate.description, 10, 60).apply {
            lineWrap = true
            wrapStyleWord = true
        }

    private val issueTypeCombo = ComboBox(metadata.issueTypes.toTypedArray())
    private val priorityCombo = ComboBox(metadata.priorities.toTypedArray())

    private val noneAssignee = JiraField("", "None")
    private val assigneeCombo = ComboBox((listOf(noneAssignee) + metadata.assignees).toTypedArray())

    private val labelsField = JBTextField(candidate.labels.joinToString(", "))
    private val parentField = JBTextField().apply { emptyText.text = "e.g. PROJ-123 (optional)" }
    private val startDateField = JBTextField().apply { emptyText.text = "YYYY-MM-DD (optional)" }
    private val dueDateField = JBTextField().apply { emptyText.text = "YYYY-MM-DD (optional)" }

    init {
        title = "Review JIRA Issue"
        setOKButtonText("Create Issue")
        issueTypeCombo.renderer = SimpleListCellRenderer.create("") { it?.name ?: "" }
        priorityCombo.renderer = SimpleListCellRenderer.create("") { it?.name ?: "" }
        assigneeCombo.renderer = SimpleListCellRenderer.create("") { it?.name ?: "None" }
        init()
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Title:", titleField)
            .addLabeledComponent("Description:", JBScrollPane(descriptionArea))
            .addLabeledComponent("Issue Type:", issueTypeCombo)
            .addLabeledComponent("Priority:", priorityCombo)
            .addLabeledComponent("Assignee:", assigneeCombo)
            .addLabeledComponent("Labels:", labelsField)
            .addLabeledComponent("Parent:", parentField)
            .addLabeledComponent("Start Date:", startDateField)
            .addLabeledComponent("Due Date:", dueDateField)
            .panel
            .also { it.preferredSize = Dimension(640, 520) }

    fun showAndGetResult(): JiraIssueRequest? {
        if (!showAndGet()) return null

        val issueType = issueTypeCombo.selectedItem as? JiraIssueType ?: return null
        val priority = priorityCombo.selectedItem as? JiraField ?: return null
        val assignee = (assigneeCombo.selectedItem as? JiraField)?.takeIf { it.id.isNotEmpty() }

        return JiraIssueRequest(
            title = titleField.text.trim(),
            description = descriptionArea.text.trim(),
            labels = labelsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            issueTypeId = issueType.id,
            priorityId = priority.id,
            assigneeAccountId = assignee?.id,
            parentIssueKey = parentField.text.trim().takeIf { it.isNotEmpty() },
            startDate = startDateField.text.trim().takeIf { it.isNotEmpty() },
            dueDate = dueDateField.text.trim().takeIf { it.isNotEmpty() },
        )
    }
}
