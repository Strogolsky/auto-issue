package com.github.strogolsky.autoissue.agent.input

import com.github.strogolsky.autoissue.context.ContextComponent
import com.github.strogolsky.autoissue.context.ContextRenderer
import com.github.strogolsky.autoissue.context.TaskInstruction

data class IssueGenerationInput(
    private val instruction: TaskInstruction,
    private val contextComponents: List<ContextComponent>,
    private val renderer: ContextRenderer,
) : AgentInput {
    override fun toPrompt(): String {
        val builder = StringBuilder()

        builder.appendLine(renderer.render(instruction))
        builder.appendLine()

        for (component in contextComponents) {
            builder.appendLine(renderer.render(component))
            builder.appendLine()
        }

        return builder.toString().trimEnd()
    }
}
