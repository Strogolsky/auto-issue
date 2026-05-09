package com.github.strogolsky.autoissue.core.context.render

import ai.koog.prompt.xml.XmlContentBuilder
import ai.koog.prompt.xml.xml
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata

class XmlPromptRenderer : PromptRenderer {
    override fun rendererKey() = "XML"

    override fun renderComponent(component: ContextComponent): String =
        xml(indented = true) {
            renderComponentInto(component)
        }

    override fun buildPrompt(block: PromptBuilder.() -> Unit): String {
        val builder = XmlPromptBuilder(this)
        builder.block()
        return builder.build()
    }

    private class XmlPromptBuilder(private val renderer: XmlPromptRenderer) : PromptBuilder {
        private val blocks = mutableListOf<XmlContentBuilder.() -> Unit>()

        override fun instruction(text: String) {
            blocks.add { tag("instructions") { text(text) } }
        }

        override fun section(
            title: String,
            content: String,
        ) {
            blocks.add { tag("section", linkedMapOf("name" to title)) { text(content) } }
        }

        override fun components(components: List<ContextComponent>) {
            if (components.isEmpty()) return
            blocks.add {
                tag("context") {
                    components.forEach { renderComponentInto(it) }
                }
            }
        }

        fun build(): String =
            xml(indented = true) {
                tag("prompt") {
                    blocks.forEach { it() }
                }
            }
    }
}

private fun XmlContentBuilder.renderComponentInto(component: ContextComponent) {
    when (component) {
        is FileContextComponent -> renderFileContext(component)
        is JiraProjectMetadata -> renderJiraMetadata(component)
        is IssueInstruction -> tag("instruction") { text(component.description) }
    }
}

private fun XmlContentBuilder.renderFileContext(c: FileContextComponent) {
    tag("file", linkedMapOf("name" to c.fileName, "language" to c.language)) {
        if (c.className != null) {
            tag("class", linkedMapOf("name" to c.className)) {
                if (c.classFields.isNotEmpty()) {
                    tag("fields") {
                        c.classFields.forEach { tag("field") { text(it) } }
                    }
                }
            }
        }
        tag("method") { cdata(c.methodBody) }
    }
}

private fun XmlContentBuilder.renderJiraMetadata(m: JiraProjectMetadata) {
    tag("jira", linkedMapOf("project-key" to m.projectKey)) {
        if (m.issueTypes.isNotEmpty()) {
            tag("issue-types") {
                m.issueTypes.forEach { issueType ->
                    tag("type", linkedMapOf("id" to issueType.id, "subtask" to issueType.subtask.toString())) {
                        text(issueType.name)
                    }
                }
            }
        }
        if (m.priorities.isNotEmpty()) {
            tag("priorities") {
                m.priorities.forEach { priority ->
                    tag("priority", linkedMapOf("id" to priority.id)) { text(priority.name) }
                }
            }
        }
        if (m.components.isNotEmpty()) {
            tag("components") {
                m.components.forEach { component ->
                    tag("component", linkedMapOf("id" to component.id)) { text(component.name) }
                }
            }
        }
        if (m.labels.isNotEmpty()) {
            tag("labels") {
                m.labels.forEach { label -> tag("label") { text(label) } }
            }
        }
    }
}
