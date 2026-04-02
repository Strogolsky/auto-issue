package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.agent.context.components.JiraField
import com.github.strogolsky.autoissue.agent.context.components.JiraIssueType
import com.github.strogolsky.autoissue.agent.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.agent.output.JiraTaskCandidate
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Base64

@Service(Service.Level.PROJECT)
class JiraApiService(private val project: Project) : Disposable {
    private val configService = project.service<JiraConfigService>()
    private val forgivingJson = Json { ignoreUnknownKeys = true }

    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            expectSuccess = true
        }

    override fun dispose() {
        httpClient.close()
        thisLogger().info("JiraApiService HTTP client closed.")
    }

    private fun HttpRequestBuilder.applyAuth(
        username: String,
        token: String,
    ) {
        val auth = Base64.getEncoder().encodeToString("$username:$token".toByteArray())
        header(HttpHeaders.Authorization, "Basic $auth")
        contentType(ContentType.Application.Json)
    }

    private fun HttpRequestBuilder.buildJiraUrl(
        baseUrl: String,
        path: String,
    ) {
        url {
            takeFrom(baseUrl)
            encodedPath = path
        }
    }

    suspend fun getMetadata(projectKey: String): JiraProjectMetadata {
        val config = configService.getEffectiveConfig()

        return try {
            val projectInfo: JsonObject =
                httpClient.get {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/project/$projectKey")
                    applyAuth(config.username, config.apiToken)
                }.body()

            val priorities: List<JiraField> =
                httpClient.get {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/priority")
                    applyAuth(config.username, config.apiToken)
                }.body()

            JiraProjectMetadata(
                projectKey = projectKey,
                projectId = projectInfo["id"]?.jsonPrimitive?.content ?: "",
                issueTypes =
                    projectInfo["issueTypes"]?.jsonArray?.map {
                        forgivingJson.decodeFromJsonElement<JiraIssueType>(it)
                    }?.filter { !it.subtask } ?: emptyList(),
                priorities = priorities,
                components =
                    projectInfo["components"]?.jsonArray?.map {
                        forgivingJson.decodeFromJsonElement<JiraField>(it)
                    } ?: emptyList(),
            )
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            thisLogger().error("Failed to get metadata. Status: ${e.response.status}. Body: $errorBody")
            throw RuntimeException("Jira API error: $errorBody", e)
        }
    }

    suspend fun createIssue(candidate: JiraTaskCandidate): String {
        val config = configService.getEffectiveConfig()

        val payload =
            buildJsonObject {
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

        return try {
            val response: JsonObject =
                httpClient.post {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/issue")
                    applyAuth(config.username, config.apiToken)
                    setBody(payload)
                }.body()

            response["key"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("Key missing in Jira response")
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            thisLogger().error("Failed to create issue. Status: ${e.response.status}. Body: $errorBody")
            throw RuntimeException("Jira API error: $errorBody", e)
        }
    }

    private fun buildAdf(text: String) =
        buildJsonObject {
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

    suspend fun testConnection(): Boolean =
        try {
            val config = configService.getEffectiveConfig()
            val response =
                httpClient.get {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/myself")
                    applyAuth(config.username, config.apiToken)
                }
            response.status.isSuccess()
        } catch (e: Exception) {
            thisLogger().warn("Jira connection test failed", e)
            false
        }
}
