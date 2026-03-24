package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.github.strogolsky.autoissue.context.*
import com.github.strogolsky.autoissue.settings.JiraConfigService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import java.util.Base64

@Service(Service.Level.PROJECT)
class JiraApiService(private val project: Project) {
    private val configService = project.service<JiraConfigService>()
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun HttpRequestBuilder.applyAuth(token: String) {
        val auth = Base64.getEncoder().encodeToString("${configService.state.username}:$token".toByteArray())
        header(HttpHeaders.Authorization, "Basic $auth")
        contentType(ContentType.Application.Json)
    }

    suspend fun getMetadata(projectKey: String): JiraProjectMetadata {
        val config = configService.getEffectiveConfig()

        val projectInfo: JsonObject = httpClient.get(config.baseUrl.removeSuffix("/") + "/rest/api/3/project/$projectKey") {
            applyAuth(config.apiToken)
        }.body()

        val priorities: List<JiraField> = httpClient.get(config.baseUrl.removeSuffix("/") + "/rest/api/3/priority") {
            applyAuth(config.apiToken)
        }.body()

        return JiraProjectMetadata(
            projectKey = projectKey,
            projectId = projectInfo["id"]?.jsonPrimitive?.content ?: "",
            issueTypes = projectInfo["issueTypes"]?.jsonArray?.map {
                Json.decodeFromJsonElement<JiraIssueType>(it)
            }?.filter { !it.subtask } ?: emptyList(),
            priorities = priorities,
            components = projectInfo["components"]?.jsonArray?.map {
                Json.decodeFromJsonElement<JiraField>(it)
            } ?: emptyList()
        )
    }

    suspend fun createIssue(candidate: JiraTaskCandidate): String {
        val config = configService.getEffectiveConfig()

        val payload = buildJsonObject {
            putJsonObject("fields") {
                putJsonObject("project") { put("key", config.projectKey) }
                put("summary", candidate.title)
                putJsonObject("issuetype") { put("id", candidate.issueTypeId) }
                putJsonObject("priority") { put("id", candidate.priorityId) }

                put("description", buildAdf(candidate.description))

                if (candidate.labels.isNotEmpty()) {
                    putJsonArray("labels") { candidate.labels.forEach { add(it) } }
                }

                if (candidate.componentIds.isNotEmpty()) {
                    putJsonArray("components") {
                        candidate.componentIds.forEach { id -> addJsonObject { put("id", id) } }
                    }
                }
            }
        }

        val response: JsonObject = httpClient.post(config.baseUrl.removeSuffix("/") + "/rest/api/3/issue") {
            applyAuth(config.apiToken)
            setBody(payload)
        }.body()

        return response["key"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Key missing in Jira response")
    }

    private fun buildAdf(text: String) = buildJsonObject {
        put("type", "doc")
        put("version", 1)
        putJsonArray("content") {
            addJsonObject {
                put("type", "paragraph")
                putJsonArray("content") {
                    addJsonObject {
                        put("type", "text")
                        put("text", text)
                    }
                }
            }
        }
    }

    suspend fun testConnection(): Boolean = try {
        val config = configService.getEffectiveConfig()
        val response = httpClient.get(config.baseUrl.removeSuffix("/") + "/rest/api/3/myself") {
            applyAuth(config.apiToken)
        }
        response.status.isSuccess()
    } catch (e: Exception) {
        false
    }
}