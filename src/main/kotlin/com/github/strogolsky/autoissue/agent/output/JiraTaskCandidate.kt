package com.github.strogolsky.autoissue.agent.output

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("JiraTaskCandidate")
@LLMDescription("A fully formatted Jira task candidate according to API v3 schema.")
data class JiraTaskCandidate(
    @property:LLMDescription("A short, clear summary (max 255 chars).")
    override val title: String,

    @property:LLMDescription("Detailed explanation in plain text (will be converted to ADF).")
    override val description: String,

    @property:LLMDescription("The ID of the issue type (e.g., '10001').")
    val issueTypeId: String,

    @property:LLMDescription("The ID of the priority (e.g., '3').")
    val priorityId: String,

    @property:LLMDescription("List of existing component IDs.")
    val componentIds: List<String> = emptyList(),

    @property:LLMDescription("List of labels (strings).")
    val labels: List<String> = emptyList(),
) : TaskCandidate
