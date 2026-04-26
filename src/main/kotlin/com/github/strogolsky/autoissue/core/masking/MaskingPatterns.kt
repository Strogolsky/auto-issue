package com.github.strogolsky.autoissue.core.masking

object MaskingPatterns {
    // Authorization: Bearer <token>
    val BEARER_TOKEN =
        MaskingPattern(
            regex =
                Regex(
                    """(?i)Authorization\s*[=:,]\s*["']?Bearer\s+([A-Za-z0-9\-._~+/]{8,500})["']?""",
                ),
        )

    // jdbc:postgresql://user:pass@host/db
    val JDBC_CONNECTION_STRING =
        MaskingPattern(
            regex = Regex("""(?i)(jdbc:[a-z0-9]+://[^\s"']{5,300})"""),
            replacement = "jdbc:<masked>",
        )

    // mongodb://user:pass@host/db  or  mongodb+srv://...
    val MONGODB_URI =
        MaskingPattern(
            regex = Regex("""(?i)(mongodb(?:\+srv)?://[^\s"']{5,300})"""),
            replacement = "mongodb://<masked>",
        )

    // redis://:password@host  or  redis://user:pass@host
    val REDIS_URI =
        MaskingPattern(
            regex = Regex("""(?i)(redis://[^\s"']{5,200})"""),
            replacement = "redis://<masked>",
        )

    // private_key = "...", privateKey = "..."
    val PRIVATE_KEY_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)private[_\-]?key\s*[=:]\s*["']([^"']{1,500})["']"""),
        )

    // password = "...", PASSWORD = "...", pwd = "..."
    val PASSWORD_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:password|passwd|pwd)\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    // secret = "...", client_secret = "...", secret_key = "..."
    val SECRET_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:\w+[_\-])?secret(?:[_\-]\w+)?\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    // apiKey = "...", api_key: "...", api-key = "..."
    val API_KEY_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:api[_\-]?key|apikey)\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    // token = "...", access_token = "...", refresh_token = "..."
    val TOKEN_ASSIGNMENT =
        MaskingPattern(
            regex = Regex("""(?i)(?:\w+[_\-])?token\s*[=:]\s*["']([^"']{1,200})["']"""),
        )

    // System.getenv("API_KEY"), ENV["SECRET_TOKEN"]
    val ENV_SECRET_LOOKUP =
        MaskingPattern(
            regex =
                Regex(
                    """(?i)(?:getenv|ENV)\s*[\[("']+([A-Z_]{3,50}(?:KEY|TOKEN|SECRET|PASSWORD|PASS|PWD)[A-Z_]*)[\])"']+""",
                ),
        )

    /** All built-in patterns in recommended application order (most-specific first). */
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
