package com.github.strogolsky.autoissue.agent.output

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("JiraTaskCandidate")
@LLMDescription("A Jira task candidate with title, description, and labels.")
data class JiraTaskCandidate(
    @property:LLMDescription("A short, clear summary (max 255 chars).")
    override val title: String,
    @property:LLMDescription("Detailed explanation in plain text (will be converted to ADF).")
    override val description: String,
    @property:LLMDescription("List of labels to apply. Use only labels from the provided available labels list.")
    val labels: List<String> = emptyList(),
) : TaskCandidate
