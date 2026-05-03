package com.github.strogolsky.autoissue.ui.components

import com.github.strogolsky.autoissue.core.context.components.JiraField
import com.github.strogolsky.autoissue.core.context.components.JiraIssueType
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.core.output.JiraIssueRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerDateModel

class IssueEditDialog(
    project: Project,
    private val candidate: JiraIssueCandidate,
    private val metadata: JiraProjectMetadata,
) : DialogWrapper(project) {
    // Base text fields
    private val titleField = JBTextField(candidate.title, 40)

    private lateinit var descriptionArea: JBTextArea

    // Dropdowns
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

    // Autocomplete fields
    private val labelsCompletionProvider =
        TextFieldWithAutoCompletion.StringsCompletionProvider(
            metadata.labels,
            null,
        )

    private val labelsField =
        TextFieldWithAutoCompletion(
            project,
            labelsCompletionProvider,
            true,
            candidate.labels.joinToString(" "),
        )

    private val parentField =
        JBTextField().apply {
            emptyText.text = "e.g. PROJ-123 (optional)"
        }

    // Date picker
    private val dueDateModel = SpinnerDateModel()
    private val dueDatePicker =
        JSpinner(dueDateModel).apply {
            editor = JSpinner.DateEditor(this, "yyyy-MM-dd")
        }

    init {
        title = "Review JIRA Issue"
        setOKButtonText("Create Issue")
        isResizable = true
        init()
    }

    override fun createCenterPanel(): JComponent =
        panel {
            row("Title:") {
                cell(titleField).align(AlignX.FILL)
            }

            row("Description:") {
                textArea()
                    .align(Align.FILL)
                    .applyToComponent {
                        descriptionArea = this
                        text = candidate.description
                        rows = 8
                        lineWrap = true
                        wrapStyleWord = true
                    }
            }.resizableRow()

            group("Issue Details") {
                row("Issue Type:") { cell(issueTypeCombo) }
                row("Priority:") { cell(priorityCombo) }
                row("Assignee:") { cell(assigneeCombo) }
            }

            group("Additional Info") {
                row("Labels:") {
                    cell(labelsField)
                        .align(AlignX.FILL)
                }
                row("Parent:") {
                    cell(parentField).align(AlignX.FILL)
                }
                row("Due Date:") {
                    cell(dueDatePicker)
                }
            }
        }

    fun showAndGetResult(): JiraIssueRequest? {
        if (!showAndGet()) return null

        val dateValue = dueDatePicker.value as? Date
        val formattedDate =
            dateValue?.toInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDate()
                ?.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return JiraIssueRequest(
            title = titleField.text.trim(),
            description = descriptionArea.text.trim(),
            labels = labelsField.text.split(Regex("[,\\s]+")).map { it.trim() }.filter { it.isNotEmpty() },
            issueTypeId = (issueTypeCombo.selectedItem as? JiraIssueType)?.id ?: return null,
            priorityId = (priorityCombo.selectedItem as? JiraField)?.id ?: return null,
            assigneeAccountId = (assigneeCombo.selectedItem as? JiraField)?.takeIf { it.id.isNotEmpty() }?.id,
            parentIssueKey = parentField.text.trim().takeIf { it.isNotEmpty() },
            dueDate = formattedDate,
        )
    }

    companion object {
        private val noneAssignee = JiraField("", "None")
    }
}
