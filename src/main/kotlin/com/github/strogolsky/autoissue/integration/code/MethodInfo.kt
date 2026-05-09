package com.github.strogolsky.autoissue.integration.code

/**
 * Information about a method in the source code.
 *
 * Used to provide context about the method where a TODO or issue was found.
 *
 * @param name The method name
 * @param signature The method signature (simplified)
 * @param body The method implementation (full source)
 */
data class MethodInfo(
    val name: String,
    val signature: String,
    val body: String,
)
