package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.agent.context.components.FileContextComponent
import com.github.strogolsky.autoissue.agent.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.agent.context.components.TaskInstruction

class XmlRendererFactory : RendererFactory {
    override fun render(component: ContextComponent): String =
        when (component) {
            is FileContextComponent -> renderFileContext(component)
            is JiraProjectMetadata -> renderJiraMetadata(component)
            is TaskInstruction -> renderTaskInstruction(component)
        }

    private fun renderFileContext(c: FileContextComponent): String =
        buildString {
            appendLine("<file-context>")
            appendLine("  <name>${c.fileName}</name>")
            appendLine("  <language>${c.language}</language>")
            if (c.className != null) {
                appendLine("  <class>${c.className}</class>")
                appendLine("  <fields>")
                c.classFields.forEach { appendLine("    <field>$it</field>") }
                appendLine("  </fields>")
            }
            appendLine("  <method-body><![CDATA[${c.methodBody}]]></method-body>")
            appendLine("</file-context>")
        }

    private fun renderJiraMetadata(m: JiraProjectMetadata): String =
        buildString {
            appendLine("<jira-metadata>")
            appendLine("  <project-key>${m.projectKey}</project-key>")
            appendLine("  <project-id>${m.projectId}</project-id>")
            appendLine("  <issue-types>")
            m.issueTypes.forEach { appendLine("    <type id=\"${it.id}\">${it.name}</type>") }
            appendLine("  </issue-types>")
            appendLine("  <priorities>")
            m.priorities.forEach { appendLine("    <priority id=\"${it.id}\">${it.name}</priority>") }
            appendLine("  </priorities>")
            appendLine("  <components>")
            m.components.forEach { appendLine("    <component id=\"${it.id}\">${it.name}</component>") }
            appendLine("  </components>")
            appendLine("</jira-metadata>")
        }

    private fun renderTaskInstruction(t: TaskInstruction): String =
        "<instruction>${t.description}</instruction>"
}
