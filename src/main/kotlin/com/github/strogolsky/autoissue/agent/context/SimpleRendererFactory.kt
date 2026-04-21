package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.agent.context.components.FileContextComponent
import com.github.strogolsky.autoissue.agent.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.agent.context.components.TaskInstruction

class SimpleRendererFactory : RendererFactory {
    override fun render(component: ContextComponent): String =
        when (component) {
            is FileContextComponent -> renderFileContext(component)
            is JiraProjectMetadata -> renderJiraMetadata(component)
            is TaskInstruction -> renderTaskInstruction(component)
        }

    private fun renderFileContext(c: FileContextComponent): String =
        buildString {
            appendLine("File: ${c.fileName}")

            if (c.className != null) {
                appendLine("Class: ${c.className}")
                appendLine("Available class fields/dependencies:")
                c.classFields.forEach { appendLine("- $it") }
            }

            appendLine("\nTarget Method Context:")
            appendLine("```${c.language}")
            appendLine(c.methodBody)
            appendLine("```")
        }

    private fun renderJiraMetadata(m: JiraProjectMetadata): String =
        buildString {
            appendLine("=== JIRA CONTEXT METADATA ===")
            appendLine("Project Key: ${m.projectKey}")
            appendLine("Project ID: ${m.projectId}")

            appendLine("\nCRITICAL INSTRUCTION FOR LLM:")
            appendLine(
                "When generating the JSON output, you MUST use the exact numerical 'ID' values from the " +
                    "lists below for 'issueTypeId', 'priorityId', and 'componentIds'. " +
                    "DO NOT use the text names.",
            )

            appendLine("\n1. AVAILABLE ISSUE TYPES (ID -> Name):")
            if (m.issueTypes.isEmpty()) {
                appendLine("   - No issue types available.")
            } else {
                m.issueTypes.forEach { type -> appendLine("   - ${type.id} -> ${type.name}") }
            }

            appendLine("\n2. AVAILABLE PRIORITIES (ID -> Name):")
            if (m.priorities.isEmpty()) {
                appendLine("   - No priorities available.")
            } else {
                m.priorities.forEach { priority -> appendLine("   - ${priority.id} -> ${priority.name}") }
            }

            appendLine("\n3. AVAILABLE COMPONENTS (ID -> Name):")
            if (m.components.isEmpty()) {
                appendLine("   - No components available.")
            } else {
                m.components.forEach { comp -> appendLine("   - ${comp.id} -> ${comp.name}") }
            }
            appendLine("=============================")
        }

    private fun renderTaskInstruction(t: TaskInstruction): String = "Instruction: ${t.description}"
}
