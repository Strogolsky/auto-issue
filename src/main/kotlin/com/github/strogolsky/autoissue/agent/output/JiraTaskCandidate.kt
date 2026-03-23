package com.github.strogolsky.autoissue.agent.output

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("JiraTaskCandidate")
@LLMDescription("A fully formatted Jira task candidate generated from source code context.")
data class JiraTaskCandidate(
    @property:LLMDescription("A short, clear summary of the issue.")
    override val title: String,

    @property:LLMDescription("Detailed explanation of the task, logic, and context formatted for Jira.")
    override val description: String,

    @property:LLMDescription("Relevant technical tags or labels (e.g., 'backend', 'performance').")
    val labels: List<String> = emptyList(),
) : TaskCandidate