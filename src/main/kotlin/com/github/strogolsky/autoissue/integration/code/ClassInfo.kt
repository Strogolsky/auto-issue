package com.github.strogolsky.autoissue.integration.code

/**
 * Information about a class in the source code.
 *
 * Used to provide context about the class where a TODO or issue was found.
 *
 * @param name The class name
 * @param fields List of field names in the class
 */
data class ClassInfo(
    val name: String,
    val fields: List<String>,
)
