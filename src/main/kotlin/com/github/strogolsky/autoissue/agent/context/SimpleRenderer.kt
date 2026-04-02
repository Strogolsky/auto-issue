package com.github.strogolsky.autoissue.agent.context

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.agent.context.components.FileContextComponent
import com.github.strogolsky.autoissue.agent.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.agent.context.components.TaskInstruction
import com.intellij.openapi.diagnostic.thisLogger

class SimpleRenderer : ContextRenderer {
    override fun render(component: ContextComponent): String {
        return when (component) {
            is TaskInstruction -> "Instruction: ${component.description}"
            is JiraProjectMetadata ->
                buildString {
                    appendLine("=== JIRA CONTEXT METADATA ===")
                    appendLine("Project Key: ${component.projectKey}")
                    appendLine("Project ID: ${component.projectId}")

                    appendLine("\nCRITICAL INSTRUCTION FOR LLM:")
                    appendLine(
                        "When generating theJSON output, you MUST use the exact numerical 'ID' values fromthe" +
                            "lists below for 'issueTypeId', 'priorityId', and 'componentIds'." +
                            "DO NOT use the text names.",
                    )

                    appendLine("\n1. AVAILABLE ISSUE TYPES (ID -> Name):")
                    if (component.issueTypes.isEmpty()) {
                        appendLine("   - No issue types available.")
                    } else {
                        component.issueTypes.forEach { type ->
                            appendLine("   - ${type.id} -> ${type.name}")
                        }
                    }

                    appendLine("\n2. AVAILABLE PRIORITIES (ID -> Name):")
                    if (component.priorities.isEmpty()) {
                        appendLine("   - No priorities available.")
                    } else {
                        component.priorities.forEach { priority ->
                            appendLine("   - ${priority.id} -> ${priority.name}")
                        }
                    }

                    appendLine("\n3. AVAILABLE COMPONENTS (ID -> Name):")
                    if (component.components.isEmpty()) {
                        appendLine("   - No components available.")
                    } else {
                        component.components.forEach { comp ->
                            appendLine("   - ${comp.id} -> ${comp.name}")
                        }
                    }
                    appendLine("=============================")
                }
            is FileContextComponent -> buildString {
                appendLine("File: ${component.fileName}")

                if (component.className != null) {
                    appendLine("Class: ${component.className}")
                    appendLine("Available class fields/dependencies:")
                    component.classFields.forEach { appendLine("- $it") }
                }

                appendLine("\nTarget Method Context:")
                appendLine("```${component.language}")
                appendLine(component.methodBody)
                appendLine("```")
            }
            else -> {
                thisLogger()
                    .debug("Rendering unknown component via fallback toString(): ${component.javaClass.simpleName}")
                component.toString()
            }
        }
    }
}
