package com.github.strogolsky.autoissue.core.context.render

import ai.koog.prompt.xml.XmlContentBuilder
import ai.koog.prompt.xml.xml
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.core.context.components.IssueInstruction
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata

/**
 * Renders prompts as structured XML.
 *
 * Formats the prompt with XML tags for clear structure:
 * ```
 * <prompt>
 *   <instructions>...</instructions>
 *   <context>
 *     <file name="..." language="...">
 *       <class name="...">
 *         <fields>...</fields>
 *       </class>
 *       <method>...</method>
 *     </file>
 *     <jira project-key="...">
 *       <issue-types>...</issue-types>
 *       <priorities>...</priorities>
 *     </jira>
 *   </context>
 * </prompt>
 * ```
 *
 * XML format is well-structured and easy for LLMs to parse with high accuracy.
 */
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

private fun XmlContentBuilder.renderFileContext(component: FileContextComponent) {
    tag("file", linkedMapOf("name" to component.fileName, "language" to component.language)) {
        if (component.className != null) {
            tag("class", linkedMapOf("name" to component.className)) {
                if (component.classFields.isNotEmpty()) {
                    tag("fields") {
                        component.classFields.forEach { tag("field") { text(it) } }
                    }
                }
            }
        }
        tag("method") { cdata(component.methodBody) }
    }
}

private fun XmlContentBuilder.renderJiraMetadata(metadata: JiraProjectMetadata) {
    tag("jira", linkedMapOf("project-key" to metadata.projectKey)) {
        if (metadata.issueTypes.isNotEmpty()) {
            tag("issue-types") {
                metadata.issueTypes.forEach { issueType ->
                    tag("type", linkedMapOf("id" to issueType.id, "subtask" to issueType.subtask.toString())) {
                        text(issueType.name)
                    }
                }
            }
        }
        if (metadata.priorities.isNotEmpty()) {
            tag("priorities") {
                metadata.priorities.forEach { priority ->
                    tag("priority", linkedMapOf("id" to priority.id)) { text(priority.name) }
                }
            }
        }
        if (metadata.components.isNotEmpty()) {
            tag("components") {
                metadata.components.forEach { component ->
                    tag("component", linkedMapOf("id" to component.id)) { text(component.name) }
                }
            }
        }
        if (metadata.labels.isNotEmpty()) {
            tag("labels") {
                metadata.labels.forEach { label -> tag("label") { text(label) } }
            }
        }
    }
}
