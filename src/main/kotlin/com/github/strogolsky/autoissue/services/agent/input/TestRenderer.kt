package com.github.strogolsky.autoissue.services.agent.input

class TestRenderer : ContextRenderer {
    override fun render(component: ContextComponent): String {
        return when (component) {
            is TaskInstruction -> "Instruction: ${component.description}"
            else -> component.toString()
        }
    }
}
