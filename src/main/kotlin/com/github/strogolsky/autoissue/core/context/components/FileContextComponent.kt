package com.github.strogolsky.autoissue.core.context.components

/**
 * Context component containing source code information about the current file and cursor location.
 *
 * Provides the AI agent with code context to understand the issue in context. Extracted by
 * FileContextComponentProvider from PSI using IntelliJ's code analysis APIs.
 *
 * Example:
 * - fileName: "UserService.kt"
 * - language: "Kotlin"
 * - imports: ["kotlin.collections.*", "com.example.model.User"]
 * - className: "UserService"
 * - classFields: ["userRepository: UserRepository", "logger: Logger"]
 * - methodSignature: "fun createUser(name: String, email: String): User"
 * - methodBody: "val user = User(...); return userRepository.save(user)"
 *
 * @param fileName Name of the source file
 * @param language Programming language (e.g., "Kotlin", "Java")
 * @param imports List of import statements from the file
 * @param className Name of the enclosing class, or null if code is not in a class
 * @param classFields List of field declarations in the enclosing class
 * @param methodSignature Signature of the enclosing method, or null if not in a method
 * @param methodBody The method body, or surrounding code context if outside a method
 */
data class FileContextComponent(
    val fileName: String,
    val language: String,
    val imports: List<String>,
    val className: String?,
    val classFields: List<String>,
    val methodSignature: String?,
    val methodBody: String,
) : ContextComponent
