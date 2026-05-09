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

/**
 * Service for communicating with JIRA REST API.
 *
 * Provides methods for:
 * - Fetching project metadata (issue types, fields, priorities)
 * - Creating issues with custom fields and attachments
 * - Testing JIRA connectivity
 * - Listing available projects
 *
 * Uses HTTP Basic authentication with username and API token.
 * Supports Jira Cloud (v3 API) with Atlassian Document Format (ADF) for descriptions.
 *
 * All operations are async/suspend functions to avoid blocking the UI.
 */
@Service(Service.Level.APP)
class JiraApiService(
    private val httpClient: HttpClient = createDefaultClient(),
    private val configService: JiraConfigService = ApplicationManager.getApplication().service<JiraConfigService>(),
) : Disposable {
    private val forgivingJson = Json { ignoreUnknownKeys = true }

    companion object {
        /**
         * Creates a default HTTP client with JSON support.
         *
         * Configured to fail on HTTP errors (4xx, 5xx status codes).
         */
        private fun createDefaultClient() =
            HttpClient(CIO) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                expectSuccess = true
            }
    }

    override fun dispose() {
        httpClient.close()
        thisLogger().info("JiraApiService HTTP client closed")
    }

    /**
     * Applies HTTP Basic authentication to a request.
     *
     * Encodes username and token in Base64 format as per HTTP Basic auth spec.
     * Also sets Content-Type to application/json.
     *
     * @param username JIRA username or email
     * @param token API token from JIRA user settings
     */
    private fun HttpRequestBuilder.applyAuth(
        username: String,
        token: String,
    ) {
        val auth = Base64.getEncoder().encodeToString("$username:$token".toByteArray())
        header(HttpHeaders.Authorization, "Basic $auth")
        contentType(ContentType.Application.Json)
    }

    /**
     * Builds a JIRA API URL from base URL and path.
     *
     * @param baseUrl The base URL (e.g., https://company.atlassian.net)
     * @param path The API path (e.g., /rest/api/3/issue)
     */
    private fun HttpRequestBuilder.buildJiraUrl(
        baseUrl: String,
        path: String,
    ) {
        url {
            takeFrom(baseUrl)
            encodedPath = path
        }
    }

    /**
     * Fetches JIRA project metadata.
     *
     * Retrieves all information needed to create an issue:
     * - Issue types (Bug, Story, Task, etc.)
     * - Field definitions
     * - Priority levels
     * - Project components
     * - Available assignees
     * - Available labels
     *
     * Failures in optional fields (assignees, labels) don't fail the entire call.
     *
     * @param projectKey The JIRA project key (e.g., "PROJ")
     * @return Complete project metadata
     * @throws JiraApiException if project or core metadata fetch fails
     */
    suspend fun getMetadata(projectKey: String): JiraProjectMetadata {
        val config = configService.getEffectiveConfig()
        thisLogger().debug("Fetching JIRA metadata for project: $projectKey from ${config.baseUrl}")

        return try {
            thisLogger().debug("Fetching project info...")
            val projectInfo: JsonObject =
                httpClient.get {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/project/$projectKey")
                    applyAuth(config.username, config.apiToken)
                }.body()
            thisLogger().debug("Project info fetched successfully")

            thisLogger().debug("Fetching priority levels...")
            val priorities: List<JiraField> =
                httpClient.get {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/priority")
                    applyAuth(config.username, config.apiToken)
                }.body()
            thisLogger().debug("Fetched ${priorities.size} priority levels")

            thisLogger().debug("Fetching assignees...")
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
                    val assigneeList =
                        response.map {
                            JiraField(
                                id = it.jsonObject["accountId"]?.jsonPrimitive?.content ?: "",
                                name = it.jsonObject["displayName"]?.jsonPrimitive?.content ?: "",
                            )
                        }
                    thisLogger().debug("Fetched ${assigneeList.size} assignees")
                    assigneeList
                } catch (e: Exception) {
                    thisLogger().warn("Failed to fetch assignees for project $projectKey. Will continue without assignee list.", e)
                    emptyList()
                }

            thisLogger().debug("Fetching available labels...")
            val labels: List<String> =
                try {
                    val response: JsonObject =
                        httpClient.get {
                            buildJiraUrl(config.baseUrl, "/rest/api/3/label")
                            applyAuth(config.username, config.apiToken)
                        }.body()
                    val labelList = response["values"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    thisLogger().debug("Fetched ${labelList.size} available labels")
                    labelList
                } catch (e: Exception) {
                    thisLogger().warn("Failed to fetch labels. Will continue without labels.", e)
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

    /**
     * Creates a new issue in JIRA.
     *
     * Constructs a JSON payload with all issue fields and sends it to the JIRA API.
     * Returns the issue key (e.g., "PROJ-123") of the created issue.
     *
     * Supports:
     * - Custom title and description
     * - Issue type and priority
     * - Assignee assignment
     * - Labels
     * - Due date
     * - Parent issue (for subtasks)
     *
     * @param request The issue creation request with all field values
     * @return The JIRA issue key of the created issue
     * @throws JiraApiException if creation fails due to API error or network issue
     */
    suspend fun createIssue(request: JiraIssueRequest): String {
        val config = configService.getEffectiveConfig()
        thisLogger().debug("Creating JIRA issue: '${request.title}' in project ${config.projectKey}")

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
            thisLogger().debug("Sending issue creation request to JIRA API...")
            val response: JsonObject =
                httpClient.post {
                    buildJiraUrl(config.baseUrl, "/rest/api/3/issue")
                    applyAuth(config.username, config.apiToken)
                    setBody(payload)
                }.body()

            val issueKey =
                response["key"]?.jsonPrimitive?.content
                    ?: error("Key missing in Jira response")
            thisLogger().info("Issue created successfully: $issueKey")
            issueKey
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            thisLogger().warn("Failed to create issue. HTTP ${e.response.status}: $errorBody")
            throw JiraApiException("Failed to create issue in Jira: ${e.message}", e)
        } catch (e: Exception) {
            thisLogger().error("Unexpected network error while creating JIRA issue", e)
            throw JiraApiException("Failed to connect to Jira. Please check your network or Base URL. Error: ${e.localizedMessage}", e)
        }
    }

    /**
     * Builds JIRA Atlassian Document Format (ADF) for issue description.
     *
     * ADF is JIRA's standard format for rich text content. This creates a simple
     * paragraph with plain text, which is then sent in the API request.
     *
     * @param text Plain text description
     * @return JSON object representing ADF document
     */
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

    /**
     * Tests connectivity to JIRA server.
     *
     * Makes a request to the /myself endpoint which requires valid authentication.
     * This is a lightweight way to verify credentials without modifying any data.
     *
     * @param baseUrl JIRA base URL
     * @param username Username or email
     * @param apiToken API token
     * @return true if connection successful, false otherwise
     */
    suspend fun testConnection(
        baseUrl: String,
        username: String,
        apiToken: String,
    ): Boolean =
        try {
            thisLogger().debug("Testing JIRA connection to $baseUrl")
            val response =
                httpClient.get {
                    buildJiraUrl(baseUrl, "/rest/api/3/myself")
                    applyAuth(username, apiToken)
                }
            val success = response.status.isSuccess()
            if (success) {
                thisLogger().info("JIRA connection test successful")
            } else {
                thisLogger().warn("JIRA connection test failed with status ${response.status}")
            }
            success
        } catch (e: Exception) {
            thisLogger().warn("JIRA connection test failed", e)
            false
        }

    /**
     * Fetches list of all JIRA projects accessible by the current user.
     *
     * Returns basic project information (key and name) for all projects.
     * Used in configuration dialogs to let users select which project to use.
     *
     * @param baseUrl JIRA base URL
     * @param username Username or email
     * @param apiToken API token
     * @return List of project summaries, or empty list if fetch fails
     */
    suspend fun getProjects(
        baseUrl: String,
        username: String,
        apiToken: String,
    ): List<JiraProjectSummary> =
        try {
            thisLogger().debug("Fetching list of JIRA projects from $baseUrl")
            val response: JsonArray =
                httpClient.get {
                    buildJiraUrl(baseUrl, "/rest/api/3/project")
                    applyAuth(username, apiToken)
                }.body()
            val projects =
                response.map {
                    JiraProjectSummary(
                        key = it.jsonObject["key"]?.jsonPrimitive?.content ?: "",
                        name = it.jsonObject["name"]?.jsonPrimitive?.content ?: "",
                    )
                }
            thisLogger().info("Fetched ${projects.size} JIRA projects")
            projects
        } catch (e: Exception) {
            thisLogger().warn("Failed to load JIRA projects", e)
            emptyList()
        }
}
