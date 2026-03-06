package com.github.strogolsky.autoissue.startup

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class MyProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val agent = createAgent()

        val result = agent.run("Hello! Could you tell me about yourself?")
        thisLogger().info("Custom AI agent say: $result")

        thisLogger().warn(
            "Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.",
        )
    }

    fun createAgent(): AIAgent<String, String> {
        val apiKey = ""

        val agent =
            AIAgent(
                promptExecutor = simpleGoogleAIExecutor(apiKey),
                llmModel = GoogleModels.Gemini2_5Flash,
            )

        return agent
    }
}
