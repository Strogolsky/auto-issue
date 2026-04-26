package com.github.strogolsky.autoissue.core.agent.strategy

import com.github.strogolsky.autoissue.core.input.IssueGenerationInput

data class AnalysisStageResult(
    val originalInput: IssueGenerationInput,
    val analysisText: String,
)
