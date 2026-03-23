package com.github.strogolsky.autoissue.agent.input

interface AgentInput {
    fun toPrompt(): String
}
