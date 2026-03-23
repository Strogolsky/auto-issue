package com.github.strogolsky.autoissue.settings

class AgentState {
    var provider: String = "GOOGLE"
    var modelName: String = "gemini-2.5-flash"
    var systemPrompt: String = "You are a developer assistant..."
    var temperature: Double = 0.0
    var maxIterations: Int = 5
    var strategyId: String = "prod-jira-strategy"
}