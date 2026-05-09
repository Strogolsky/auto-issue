package com.github.strogolsky.autoissue.core.masking

/**
 * Pre-defined patterns for masking sensitive information in code context.
 *
 * When sharing code with the AI agent, sensitive data like API keys, passwords,
 * and connection strings should be masked to prevent exposure. This object provides
 * regex patterns to detect and mask common types of secrets.
 *
 * All patterns are applied in order during context masking.
 */
object MaskingPatterns {
    /**
     * HTTP Bearer token pattern.
     * Matches: Authorization: Bearer <token> or Authorization = Bearer <token>
     */
    val BEARER_TOKEN =
        MaskingPattern(
            regex =
                Regex(
                    """(?i)Authorization\s*[=:,]\s*["']?Bearer\s+([A-Za-z0-9\-._~+/]{8,500})["']?""",
                ),
        )

    /** JDBC connection strings like jdbc:postgresql://user:pass@host/db */
    val JDBC_CONNECTION_STRING =
        MaskingPattern(
            regex = Regex("""(?i)(jdbc:[a-z0-9]+://[^\s"']{5,300})"""),
            replacement = "jdbc:<masked>",
        )

    /** MongoDB URIs like mongodb://user:pass@host/db or mongodb+srv://... */
    val MONGODB_URI =
        MaskingPattern(
            regex = Regex("""(?i)(mongodb(?:\+srv)?://[^\s"']{5,300})"""),
            replacement = "mongodb://<masked>",
        )

    /** Redis URIs like redis://:password@host or redis://user:pass@host */
    val REDIS_URI =
        MaskingPattern(
            regex = Regex("""(?i)(redis://[^\s"']{5,200})"""),
            replacement = "redis://<masked>",
        )

    /** Private key assignments like private_key = "..." or privateKey = "..." */
    val PRIVATE_KEY_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)private[_\-]?key\s*[=:]\s*["']([^"']{1,500})["']"""),
        )

    /** Password assignments like password = "...", PASSWORD = "...", pwd = "..." */
    val PASSWORD_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:password|passwd|pwd)\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    /** Secret assignments like secret = "...", client_secret = "...", secret_key = "..." */
    val SECRET_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:\w+[_\-])?secret(?:[_\-]\w+)?\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    /** API key assignments like apiKey = "...", api_key: "...", api-key = "..." */
    val API_KEY_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:api[_\-]?key|apikey)\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    /** Token assignments like token = "...", access_token = "...", refresh_token = "..." */
    val TOKEN_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:\w+[_\-])?token\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    /** Environment variable lookups like System.getenv("API_KEY"), ENV["SECRET_TOKEN"] */
    val ENV_SECRET_LOOKUP =
        MaskingPattern(
            regex =
                Regex(
                    """(?i)(?:getenv|ENV)\s*[\[("']+([A-Z_]{3,50}(?:KEY|TOKEN|SECRET|PASSWORD|PASS|PWD)[A-Z_]*)[])"']+""",
                ),
        )

    /**
     * All built-in patterns in recommended application order (most-specific first).
     *
     * Patterns should be applied in this order to avoid conflicts and ensure
     * the most specific patterns are applied before more general ones.
     */
    val ALL: List<MaskingPattern> =
        listOf(
            BEARER_TOKEN,
            JDBC_CONNECTION_STRING,
            MONGODB_URI,
            REDIS_URI,
            PRIVATE_KEY_ASSIGNMENT,
            PASSWORD_ASSIGNMENT,
            SECRET_ASSIGNMENT,
            API_KEY_ASSIGNMENT,
            TOKEN_ASSIGNMENT,
            ENV_SECRET_LOOKUP,
        )
}
