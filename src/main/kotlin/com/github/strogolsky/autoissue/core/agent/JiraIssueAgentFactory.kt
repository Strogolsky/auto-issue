package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.integration.code.tools.ReadFileContentTool
import com.github.strogolsky.autoissue.integration.code.tools.SearchFilesTool
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfig
import com.github.strogolsky.autoissue.plugin.startup.LangfuseConfigLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlin.time.ExperimentalTime

@Service(Service.Level.PROJECT)
class JiraIssueAgentFactory(private val project: Project) :
    AgentFactory<KoogAgentAdapter<IssueGenerationInput, JiraIssueCandidate>> {
    @OptIn(ExperimentalTime::class)
    override fun createAgent(config: LlmAgentConfig): KoogAgentAdapter<IssueGenerationInput, JiraIssueCandidate> {
        thisLogger().info("Creating Jira Issue Agent...")

        val providerRegistry = project.service<LlmProviderRegistry>()
        val strategyRegistry = ApplicationManager.getApplication().service<JiraStrategyRegistry>()

        val provider = providerRegistry.getProvider(config.provider)
        val executor = provider.createExecutor(config.apiKey)

        val factory =
            strategyRegistry.findFactory(config.provider, config.strategyId)
                ?: error("Strategy '${config.strategyId}' not found for provider '${config.provider}'")
        val strategy = factory.createStrategy(project)

        thisLogger().debug(
            "Agent components resolved. Provider: ${config.provider}, Strategy: ${config.strategyId}",
        )

        val toolRegistry = ToolRegistry {
            tools(SearchFilesTool(project))
            tools(ReadFileContentTool(project))
        }

        val langfuseConfig = LangfuseConfigLoader.load()

        val rawKoogAgent =
            AIAgent(
                promptExecutor = executor,
                llmModel = provider.defaultModel,
                strategy = strategy,
                systemPrompt = config.systemPrompt,
                temperature = config.temperature,
                maxIterations = config.maxIterations,
                toolRegistry = toolRegistry,
                installFeatures = {
                    langfuseConfig?.let {
                        install(OpenTelemetry) {
                            setServiceInfo("auto-issue", "0.0.1")
                            addLangfuseExporter(
                                langfuseUrl = it.url,
                                langfusePublicKey = it.publicKey,
                                langfuseSecretKey = it.secretKey,
                            )
                            setVerbose(true)
                        }
                    }
                },
            )

        thisLogger().info("Jira Issue Agent successfully created.")
        return KoogAgentAdapter(rawKoogAgent)
    }
}
