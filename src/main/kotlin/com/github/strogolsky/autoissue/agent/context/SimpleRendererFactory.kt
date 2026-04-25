package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.agent.context.components.FileContextComponent
import com.github.strogolsky.autoissue.agent.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.agent.context.components.TaskInstruction
import com.github.strogolsky.autoissue.masking.ContentMasker

class SimpleRendererFactory(private val masker: ContentMasker) : RendererFactory {
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
                c.classFields.map { masker.mask(it) }.forEach { appendLine("- $it") }
            }

            appendLine("\nTarget Method Context:")
            appendLine("```${c.language}")
            appendLine(masker.mask(c.methodBody))
            appendLine("```")
        }

    private fun renderJiraMetadata(m: JiraProjectMetadata): String =
        buildString {
            appendLine("=== JIRA CONTEXT METADATA ===")
            appendLine("Project Key: ${m.projectKey}")

            appendLine("\nAVAILABLE LABELS:")
            appendLine("Use only labels from this list for the 'labels' field (or leave it empty):")
            if (m.labels.isEmpty()) {
                appendLine("   - No labels available.")
            } else {
                m.labels.forEach { appendLine("   - $it") }
            }
            appendLine("=============================")
        }

    private fun renderTaskInstruction(t: TaskInstruction): String = "Instruction: ${masker.mask(t.description)}"
}
