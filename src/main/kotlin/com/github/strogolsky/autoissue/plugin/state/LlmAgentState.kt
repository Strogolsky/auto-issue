package com.github.strogolsky.autoissue.plugin.state

data class LlmAgentState(
    var provider: String = "",
    var modelName: String = "",
    var systemPrompt: String = "",
    var temperature: Double = 0.0,
    var maxIterations: Int = 0,
    var strategyId: String = "",
)
