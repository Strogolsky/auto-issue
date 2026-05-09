package com.github.strogolsky.autoissue.core.context.render

import ai.koog.prompt.markdown.MarkdownContentBuilder
import ai.koog.prompt.markdown.markdown
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata

class MarkdownPromptRenderer : PromptRenderer {
    override fun rendererKey() = "MARKDOWN"

    override fun renderComponent(component: ContextComponent): String =
        markdown {
            when (component) {
                is FileContextComponent -> renderFileContext(component)
                is JiraProjectMetadata -> renderJiraMetadata(component)
                is IssueInstruction -> +"Instruction: ${component.description}"
            }
        }

    override fun buildPrompt(block: PromptBuilder.() -> Unit): String {
        val builder = MarkdownPromptBuilder(this)
        builder.block()
        return builder.build()
    }

    private class MarkdownPromptBuilder(private val renderer: MarkdownPromptRenderer) : PromptBuilder {
        private val blocks = mutableListOf<MarkdownContentBuilder.() -> Unit>()

        override fun instruction(text: String) {
            blocks.add {
                h2("Instructions")
                +text
            }
        }

        override fun section(
            title: String,
            content: String,
        ) {
            blocks.add {
                h2(title)
                +content
            }
        }

        override fun components(components: List<ContextComponent>) {
            if (components.isEmpty()) return
            blocks.add {
                h2("Context")
                components.forEach { component ->
                    when (component) {
                        is FileContextComponent -> renderFileContext(component)
                        is JiraProjectMetadata -> renderJiraMetadata(component)
                        is IssueInstruction -> +"Instruction: ${component.description}"
                    }
                }
            }
        }

        fun build(): String = markdown { blocks.forEach { it() } }.trimEnd()
    }
}

private fun MarkdownContentBuilder.renderFileContext(c: FileContextComponent) {
    h3(c.fileName)

    if (c.className != null) {
        +"Class: ${c.className}"
        if (c.classFields.isNotEmpty()) {
            bulleted {
                c.classFields.forEach { item { +it } }
            }
        }
    }

    codeblock(c.methodBody, c.language)
}

private fun MarkdownContentBuilder.renderJiraMetadata(m: JiraProjectMetadata) {
    h3("Jira Context")
    +"Project Key: ${m.projectKey}"

    if (m.labels.isNotEmpty()) {
        +"Available Labels:"
        bulleted {
            m.labels.forEach { item { +it } }
        }
    }
}
