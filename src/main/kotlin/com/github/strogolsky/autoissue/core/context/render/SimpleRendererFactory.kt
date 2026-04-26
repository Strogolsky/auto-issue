package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.core.context.components.TaskInstruction
import com.github.strogolsky.autoissue.core.masking.ContentMasker

class SimpleRendererFactory(private val masker: ContentMasker) : RendererFactory {
    override fun renderComponent(component: ContextComponent): String =
        when (component) {
            is FileContextComponent -> renderFileContext(component)
            is JiraProjectMetadata -> renderJiraMetadata(component)
            is TaskInstruction -> renderTaskInstruction(component)
        }

    override fun buildPrompt(block: PromptBuilder.() -> Unit): String {
        val builder = SimplePromptBuilder(this)
        builder.block()
        return builder.build()
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

    private class SimplePromptBuilder(private val factory: SimpleRendererFactory) : PromptBuilder {
        private val sb = StringBuilder()

        override fun instruction(text: String) {
            sb.appendLine("=== INSTRUCTIONS ===")
            sb.appendLine(text).appendLine()
        }

        override fun section(
            title: String,
            content: String,
        ) {
            sb.appendLine("=== ${title.uppercase()} ===")
            sb.appendLine(content).appendLine()
        }

        override fun components(components: List<ContextComponent>) {
            if (components.isEmpty()) return
            sb.appendLine("=== CONTEXT ===")
            components.forEach { sb.appendLine(factory.renderComponent(it)) }
            sb.appendLine()
        }

        fun build(): String = sb.toString().trimEnd()
    }
}