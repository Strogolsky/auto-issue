package com.github.strogolsky.autoissue.integration.code

/**
 * Comprehensive file and code context information.
 *
 * Contains detailed information about the code location where an issue was detected,
 * including file metadata, imports, enclosing class/method, and surrounding code.
 * This context is provided to the AI agent to generate better issues.
 *
 * @param fileName Name of the source file
 * @param language Programming language (e.g., "java", "kotlin")
 * @param imports List of import statements in the file
 * @param enclosingClass Information about the class containing the issue (if any)
 * @param enclosingMethod Information about the method containing the issue (if any)
 * @param surroundingText Source code lines around the issue location (for context)
 */
data class DetailedFileInfo(
    val fileName: String,
    val language: String,
    val imports: List<String>,
    val enclosingClass: ClassInfo?,
    val enclosingMethod: MethodInfo?,
    val surroundingText: String,
)
