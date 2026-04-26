package com.github.strogolsky.autoissue.core.input

import com.github.strogolsky.autoissue.core.context.components.ContextComponent

data class IssueGenerationInput(
    val components: List<ContextComponent>,
)
