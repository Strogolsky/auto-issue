package com.github.strogolsky.autoissue.core.output

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AI-generated JIRA issue candidate.
 *
 * This is the primary output from the AI agent. It contains:
 * - Issue title (summary)
 * - Detailed description
 * - Recommended labels
 *
 * The user can review and edit these values in the issue dialog before creation.
 *
 * The @LLMDescription annotations guide the AI model on what each field should contain.
 *
 * @param title A short, clear summary of the issue (max 255 characters)
 * @param description Detailed explanation of the issue in plain text
 * @param labels List of labels to apply (selected from project's available labels)
 */
@Serializable
@SerialName("JiraIssueCandidate")
@LLMDescription("A Jira issue candidate with title, description, and labels.")
data class JiraIssueCandidate(
    @property:LLMDescription("A short, clear summary (max 255 chars).")
    override val title: String,
    @property:LLMDescription("Detailed explanation in plain text (will be converted to ADF).")
    override val description: String,
    @property:LLMDescription("List of labels to apply. Use only labels from the provided available labels list.")
    val labels: List<String> = emptyList(),
) : IssueCandidate
