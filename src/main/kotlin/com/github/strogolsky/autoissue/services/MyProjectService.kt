package com.github.strogolsky.autoissue.services

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.github.strogolsky.autoissue.MyBundle
import com.github.strogolsky.autoissue.services.agent.KoogIssueGenerationAgent
import com.github.strogolsky.autoissue.services.agent.input.IssueGenerationInput
import com.github.strogolsky.autoissue.services.agent.input.TaskInstruction
import com.github.strogolsky.autoissue.services.agent.input.TestRenderer
import com.github.strogolsky.autoissue.services.agent.output.TestTaskCandidate
import com.github.strogolsky.autoissue.services.agent.strategy.TestIssueStrategyFactory
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.jetbrains.fus.reporting.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    init {
        thisLogger().info(MyBundle.message("projectService", project.name))

    }

    fun getRandomNumber() = (1..100).random()
}
