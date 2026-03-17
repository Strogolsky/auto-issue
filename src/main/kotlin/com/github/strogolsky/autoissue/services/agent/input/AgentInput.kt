package com.github.strogolsky.autoissue.services.agent.input

interface AgentInput {
    fun toPrompt(): String
}
