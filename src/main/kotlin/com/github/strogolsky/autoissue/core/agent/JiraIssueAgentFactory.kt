package com.github.strogolsky.autoissue.core.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import com.github.strogolsky.autoissue.core.agent.strategy.JiraStrategyRegistry
import com.github.strogolsky.autoissue.core.agent.tools.ListAllClassesTool
import com.github.strogolsky.autoissue.core.agent.tools.ListProjectFilesTool
import com.github.strogolsky.autoissue.core.agent.tools.ReadFileContentTool
import com.github.strogolsky.autoissue.core.agent.tools.SearchSymbolTool
import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.github.strogolsky.autoissue.plugin.config.LlmAgentConfig
import com.github.strogolsky.autoissue.plugin.startup.LangfuseConfigLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlin.time.ExperimentalTime

/**
 * Factory for creating JIRA issue generation agents.
 *
 * Configures and instantiates agents with:
 * - LLM provider (Google, Anthropic, etc.)
 * - Generation strategy (direct, reasoning, etc.)
 * - Tool registry (class listing, file reading, symbol search, etc.)
 * - Optional Langfuse observability integration for monitoring
 *
 * The factory handles all the wiring needed to create a fully functional
 * agent capable of generating JIRA issues based on project context.
 */
@Service(Service.Level.PROJECT)
class JiraIssueAgentFactory(private val project: Project) :
    AgentFactory<KoogAgentAdapter<IssueGenerationInput, JiraIssueCandidate>> {
    /**
     * Creates a fully configured JIRA issue generation agent.
     *
     * Process:
     * 1. Resolve LLM provider based on config
     * 2. Create executor for the provider with API key
     * 3. Resolve generation strategy from registry
     * 4. Create tool registry with code analysis tools
     * 5. Load optional Langfuse config for monitoring
     * 6. Instantiate Koog AI agent with all components
     * 7. Wrap with KoogAgentAdapter for type compatibility
     *
     * @param config The LLM configuration with provider, strategy, and parameters
     * @return Configured agent ready for issue generation
     * @throws IllegalArgumentException if strategy is not found for the provider
     */
    @OptIn(ExperimentalTime::class)
    override fun createAgent(config: LlmAgentConfig): KoogAgentAdapter<IssueGenerationInput, JiraIssueCandidate> {
        thisLogger().info("Creating Jira Issue Agent with config: provider=${config.provider}, strategy=${config.strategyId}...")

        // Resolve LLM provider and create executor
        val providerRegistry = ApplicationManager.getApplication().service<LlmProviderRegistry>()
        val strategyRegistry = ApplicationManager.getApplication().service<JiraStrategyRegistry>()

        thisLogger().debug("Resolving LLM provider: ${config.provider}")
        val provider = providerRegistry.getProvider(config.provider)
        val executor = provider.createExecutor(config.apiKey)
        thisLogger().debug("LLM provider resolved. Default model: ${provider.defaultModel}")

        // Resolve generation strategy
        thisLogger().debug("Resolving strategy: ${config.strategyId} for provider: ${config.provider}")
        val factory =
            strategyRegistry.findFactory(config.provider, config.strategyId)
                ?: error("Strategy '${config.strategyId}' not found for provider '${config.provider}'")
        val strategy = factory.createStrategy(project)
        thisLogger().debug("Strategy resolved: ${strategy.javaClass.simpleName}")

        // Create tool registry for code analysis
        thisLogger().debug("Initializing agent tools...")
        val toolRegistry =
            ToolRegistry {
                tools(ListAllClassesTool(project))
                tools(SearchSymbolTool(project))
                tools(ListProjectFilesTool(project))
                tools(ReadFileContentTool(project))
            }
        thisLogger().debug("Tool registry created with 4 tools")

        // Load optional observability configuration
        thisLogger().debug("Loading Langfuse observability configuration...")
        val langfuseConfig = LangfuseConfigLoader.load()
        if (langfuseConfig != null) {
            thisLogger().debug("Langfuse configured: ${langfuseConfig.url}")
        } else {
            thisLogger().debug("Langfuse not configured - observability disabled")
        }

        // Instantiate Koog AI agent with all components
        thisLogger().debug("Creating Koog AI agent with temperature=${config.temperature}, maxIterations=${config.maxIterations}")
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
                        thisLogger().debug("Installing OpenTelemetry with Langfuse exporter")
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

        thisLogger().info("Jira Issue Agent successfully created and configured")
        return KoogAgentAdapter(rawKoogAgent)
    }
}
