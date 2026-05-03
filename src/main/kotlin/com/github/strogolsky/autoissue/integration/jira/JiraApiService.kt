package com.github.strogolsky.autoissue.integration.jira

import com.github.strogolsky.autoissue.core.context.components.JiraField
import com.github.strogolsky.autoissue.core.context.components.JiraIssueType
import com.github.strogolsky.autoissue.core.context.components.JiraProjectMetadata
import com.github.strogolsky.autoissue.core.exceptions.JiraApiException
import com.github.strogolsky.autoissue.core.output.JiraIssueRequest
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import com.github.strogolsky.autoissue.plugin.config.JiraProjectSummary
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Base64

@Service(Service.Level.APP)
class JiraApiService : Disposable {
    private val configService = ApplicationManager.getApplication().service<JiraConfigService>()
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

            val assignees: List<JiraField> =
                try {
                    val response: JsonArray =
                        httpClient.get {
                            url {
                                takeFrom(config.baseUrl)
                                encodedPath = "/rest/api/3/user/assignable/search"
                                parameters.append("project", projectKey)
                                parameters.append("maxResults", "100")
                            }
                            applyAuth(config.username, config.apiToken)
                        }.body()
                    response.map {
                        JiraField(
                            id = it.jsonObject["accountId"]?.jsonPrimitive?.content ?: "",
                            name = it.jsonObject["displayName"]?.jsonPrimitive?.content ?: "",
                        )
                    }
                } catch (e: Exception) {
                    thisLogger().warn("Failed to fetch assignees for project $projectKey", e)
                    emptyList()
                }

            val labels: List<String> =
                try {
                    val response: JsonObject =
                        httpClient.get {
                            buildJiraUrl(config.baseUrl, "/rest/api/3/label")
                            applyAuth(config.username, config.apiToken)
                        }.body()
                    response["values"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                } catch (e: Exception) {
                    thisLogger().warn("Failed to fetch labels", e)
                    emptyList()
                }

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
                assignees = assignees,
                labels = labels,
            )
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            thisLogger().error("Failed to get metadata. Status: ${e.response.status}. Body: $errorBody")
            throw JiraApiException("Jira API error (HTTP ${e.response.status}): $errorBody", e)
        } catch (e: Exception) {
            thisLogger().error("Unexpected network error while fetching Jira metadata", e)
            throw JiraApiException("Failed to connect to Jira. Please check your network or Base URL. Error: ${e.localizedMessage}", e)
        }
    }

    suspend fun createIssue(request: JiraIssueRequest): String {
        val config = configService.getEffectiveConfig()

        val payload =
            buildJsonObject {
                putJsonObject("fields") {
                    putJsonObject("project") { put("key", config.projectKey) }
                    put("summary", request.title)
                    putJsonObject("issuetype") { put("id", request.issueTypeId) }
                    putJsonObject("priority") { put("id", request.priorityId) }
                    put("description", buildAdf(request.description))

                    if (request.labels.isNotEmpty()) {
                        putJsonArray("labels") { request.labels.forEach { add(it) } }
                    }

                    request.assigneeAccountId?.let { accountId ->
                        putJsonObject("assignee") { put("accountId", accountId) }
                    }

                    request.parentIssueKey?.let { key ->
                        putJsonObject("parent") { put("key", key) }
                    }

                    request.dueDate?.let { put("duedate", it) }
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
                ?: error("Key missing in Jira response")
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            thisLogger().error("Failed to create issue. Status: ${e.response.status}. Body: $errorBody")
            throw JiraApiException("Failed to create issue in Jira: ${e.message}", e)
        } catch (e: Exception) {
            thisLogger().error("Unexpected network error while fetching Jira metadata", e)
            throw JiraApiException("Failed to connect to Jira. Please check your network or Base URL. Error: ${e.localizedMessage}", e)
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

    suspend fun testConnection(
        baseUrl: String,
        username: String,
        apiToken: String,
    ): Boolean =
        try {
            val response =
                httpClient.get {
                    buildJiraUrl(baseUrl, "/rest/api/3/myself")
                    applyAuth(username, apiToken)
                }
            response.status.isSuccess()
        } catch (e: Exception) {
            thisLogger().warn("Jira connection test failed", e)
            false
        }

    suspend fun getProjects(
        baseUrl: String,
        username: String,
        apiToken: String,
    ): List<JiraProjectSummary> =
        try {
            val response: JsonArray =
                httpClient.get {
                    buildJiraUrl(baseUrl, "/rest/api/3/project")
                    applyAuth(username, apiToken)
                }.body()
            response.map {
                JiraProjectSummary(
                    key = it.jsonObject["key"]?.jsonPrimitive?.content ?: "",
                    name = it.jsonObject["name"]?.jsonPrimitive?.content ?: "",
                )
            }
        } catch (e: Exception) {
            thisLogger().warn("Failed to load Jira projects", e)
            emptyList()
        }
}
