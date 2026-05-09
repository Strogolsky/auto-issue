package com.github.strogolsky.autoissue.core.context.components

/**
 * User's instruction for the issue to be created.
 *
 * This is the primary user input - what they want the issue to be about.
 * It becomes a context component that the AI agent uses to understand
 * the intent and generate an appropriate issue title and description.
 *
 * Examples:
 * - "Fix slow database queries in user service"
 * - "Add validation for email field in registration form"
 * - "Refactor authentication module"
 *
 * @param description The user's instruction text
 */
data class IssueInstruction(val description: String) : ContextComponent
