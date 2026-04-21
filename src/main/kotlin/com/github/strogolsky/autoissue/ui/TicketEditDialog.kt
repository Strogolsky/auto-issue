package com.github.strogolsky.autoissue.ui

import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import javax.swing.JComponent

class TicketEditDialog(
    project: Project,
    private val candidate: JiraTaskCandidate,
) : DialogWrapper(project) {

    private val titleField = JBTextField(candidate.title)
    private val descriptionArea = JBTextArea(candidate.description, 10, 60).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    init {
        title = "Review JIRA Issue"
        setOKButtonText("Create Issue")
        init()
    }

    override fun createCenterPanel(): JComponent =
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Title:", titleField)
            .addLabeledComponent("Description:", JBScrollPane(descriptionArea))
            .panel
            .also { it.preferredSize = Dimension(640, 400) }

    fun showAndGetResult(): JiraTaskCandidate? =
        if (showAndGet()) {
            candidate.copy(
                title = titleField.text.trim(),
                description = descriptionArea.text.trim(),
            )
        } else {
            null
        }
}
