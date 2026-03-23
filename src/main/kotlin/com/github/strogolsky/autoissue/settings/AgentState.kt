package com.github.strogolsky.autoissue.settings

class AgentState {
    var provider: String = "GOOGLE"
    var modelName: String = "gemini-2.5-flash"
    var systemPrompt: String = "You are an expert developer assistant. Analyze the context and generate a Jira task."
    var temperature: Double = 0.0
    var maxIterations: Int = 5
    var strategyId: String = "prod-jira-strategy"
}
