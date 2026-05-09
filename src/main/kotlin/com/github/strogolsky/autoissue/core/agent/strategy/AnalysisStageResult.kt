package com.github.strogolsky.autoissue.core.agent.strategy

import com.github.strogolsky.autoissue.core.input.IssueGenerationInput

/**
 * Result from the analysis stage of the reasoning strategy.
 *
 * Carries both the original input context and the LLM-generated analysis text
 * to the structuring stage. The analysis text contains the LLM's technical analysis
 * of the TODO and surrounding code, which helps generate a better-informed issue.
 *
 * Used only in JiraReasoningStrategyFactory's multi-stage approach.
 *
 * @param originalInput The original IssueGenerationInput (preserved for structuring stage)
 * @param analysisText The LLM-generated analysis of the TODO and code context
 */
data class AnalysisStageResult(
    val originalInput: IssueGenerationInput,
    val analysisText: String,
)
