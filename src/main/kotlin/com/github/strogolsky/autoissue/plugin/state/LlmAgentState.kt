package com.github.strogolsky.autoissue.plugin.state

data class LlmAgentState(
    var provider: String = "GOOGLE",
    var modelName: String = "gemini-2.5-flash-lite",
    var systemPrompt: String =
        "You are a Jira issue generator. Based on the provided code context " +
            "and TODO comment, generate a well-structured Jira issue.",
    var temperature: Double = 0.0,
    var maxIterations: Int = 20,
    var strategyId: String = "prod-jira-strategy",
)
