package com.github.strogolsky.autoissue.core.masking

/**
 * Configuration for the content masking system.
 *
 * @param enabled Whether content masking is enabled (default: true)
 *                Disabling allows for debugging but exposes sensitive data to the LLM
 */
data class MaskingConfig(
    val enabled: Boolean = true,
)
