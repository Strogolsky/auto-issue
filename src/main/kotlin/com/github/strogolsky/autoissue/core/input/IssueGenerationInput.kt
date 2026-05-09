package com.github.strogolsky.autoissue.core.input

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

/**
 * Input data for the AI agent to generate a JIRA issue.
 *
 * Contains all context components that the agent needs:
 * - User instruction (what the issue should be about)
 * - Project structure information
 * - JIRA project metadata (issue types, fields, etc.)
 * - Code context (surrounding code, class/method info)
 *
 * The agent processes these components to generate an appropriate issue title, description, and labels.
 *
 * @param components List of context components providing information to the AI
 */
data class IssueGenerationInput(
    val components: List<ContextComponent>,
)
