package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.plugin.config.LangfuseConfig

object LangfuseConfigLoader {
    fun load(): LangfuseConfig? {
        val url = System.getProperty("autoissue.langfuse.url")?.takeIf { it.isNotBlank() } ?: return null
        val publicKey = System.getProperty("autoissue.langfuse.public-key")?.takeIf { it.isNotBlank() } ?: return null
        val secretKey = System.getProperty("autoissue.langfuse.secret-key")?.takeIf { it.isNotBlank() } ?: return null
        return LangfuseConfig(url, publicKey, secretKey)
    }
}
