package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata

class PlainTextPromptRenderer : PromptRenderer {
    override fun rendererKey() = "SIMPLE"

    override fun renderComponent(component: ContextComponent): String =
        when (component) {
            is FileContextComponent -> renderFileContext(component)
            is JiraProjectMetadata -> renderJiraMetadata(component)
            is IssueInstruction -> renderIssueInstruction(component)
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

            appendLine("\nAVAILABLE LABELS:")
            appendLine("Use only labels from this list for the 'labels' field (or leave it empty):")
            if (m.labels.isEmpty()) {
                appendLine("   - No labels available.")
            } else {
                m.labels.forEach { appendLine("   - $it") }
            }
            appendLine("=============================")
        }

    private fun renderIssueInstruction(t: IssueInstruction): String = "Instruction: ${t.description}"

    private class SimplePromptBuilder(private val renderer: PlainTextPromptRenderer) : PromptBuilder {
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
            components.forEach { sb.appendLine(renderer.renderComponent(it)) }
            sb.appendLine()
        }

        fun build(): String = sb.toString().trimEnd()
    }
}
