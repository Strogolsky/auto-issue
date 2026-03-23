package com.github.strogolsky.autoissue.context

import com.intellij.openapi.diagnostic.thisLogger

class SimpleRenderer : ContextRenderer {
    override fun render(component: ContextComponent): String {
        return when (component) {
            is TaskInstruction -> "Instruction: ${component.description}"
            is JiraProjectMetadata -> "Jira Context: Project Key is '${component.projectKey}'"
            else -> {
                thisLogger().debug("Rendering unknown component via fallback toString(): ${component.javaClass.simpleName}")
                component.toString()
            }
        }
    }
}