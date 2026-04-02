package com.github.strogolsky.autoissue.agent.input

import com.github.strogolsky.autoissue.agent.context.components.ContextComponent
import com.github.strogolsky.autoissue.agent.context.ContextRenderer
import com.github.strogolsky.autoissue.agent.context.components.TaskInstruction
import com.intellij.openapi.diagnostic.thisLogger

data class IssueGenerationInput(
    private val instruction: TaskInstruction,
    private val contextComponents: List<ContextComponent>,
    private val renderer: ContextRenderer,
) : AgentInput {
    override fun toPrompt(): String {
        thisLogger().debug("Starting prompt generation with ${contextComponents.size} context components.")

        val prompt =
            buildString {
                appendLine(renderer.render(instruction))
                appendLine()

                for (component in contextComponents) {
                    appendLine(renderer.render(component))
                    appendLine()
                }
            }

        thisLogger().debug("Prompt generation completed. Total length: ${prompt.length} characters.")
        return prompt.trimEnd()
    }
}
