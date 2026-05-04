package com.github.strogolsky.autoissue.integration.jira

import com.github.strogolsky.autoissue.core.exceptions.JiraApiException
import com.github.strogolsky.autoissue.core.output.JiraIssueRequest
import com.github.strogolsky.autoissue.plugin.config.JiraConfig
import com.github.strogolsky.autoissue.plugin.config.JiraConfigService
import io.ktor.client.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JiraApiServiceTest {

    private val mockConfigService = mockk<JiraConfigService> {
        every { getEffectiveConfig() } returns JiraConfig(
            baseUrl = "https://test.atlassian.net",
            username = "admin",
            apiToken = "p@ssword",
            projectKey = "PROJ"
        )
    }

    private fun createService(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): JiraApiService {
        val mockEngine = MockEngine(handler)
        val mockClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
            expectSuccess = true
        }
        return JiraApiService(mockClient, mockConfigService)
    }

    @Test
    fun should_ReturnKey_When_IssueCreatedSuccessfully() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE: Mock 201 Created response.
        val service = createService {
            respond(
                content = """{"key":"TASK-101"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val issueReq = JiraIssueRequest(
            "Bug title",
            "Bug description",
            emptyList(),
            "10001",
            "3",
            null,
            null,
            null
        )

        // 2. ACT: Call createIssue.
        val key = service.createIssue(issueReq)

        // 3. ASSERT: Verify the returned key.
        assertEquals("TASK-101", key)
    }

    @Test
    fun should_AssembleFullMetadata_When_AllEndpointsSucceed() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE: Mock complex response for metadata gathering.
        val service = createService { request ->
            when {
                request.url.encodedPath.contains("/project/PROJ") ->
                    respond("""{"id":"101", "issueTypes":[{"id":"1", "name":"Bug", "subtask":false}], "components":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("/priority") ->
                    respond("""[{"id":"1", "name":"High"}]""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("/user/assignable/search") ->
                    respond("""[{"accountId":"acc-1", "displayName":"John"}]""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("/label") ->
                    respond("""{"values":["bug", "ui"]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }

        // 2. ACT: Fetch metadata.
        val metadata = service.getMetadata("PROJ")

        // 3. ASSERT: Verify structural integrity of collected data.
        assertEquals("PROJ", metadata.projectKey)
        assertEquals("101", metadata.projectId)
        assertEquals(1, metadata.issueTypes.size)
        assertEquals("acc-1", metadata.assignees.first().id)
        assertTrue(metadata.labels.contains("bug"))
    }

    @Test
    fun should_ThrowJiraApiException_When_JiraReturns401() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE: Mock 401 response.
        val service = createService { respond("Unauthorized", HttpStatusCode.Unauthorized) }
        val issueReq = JiraIssueRequest("T", "D", emptyList(), "1", "1", null, null, null)

        // 2. ACT & ASSERT: Verify exception wrapping.
        val exception = assertThrows(JiraApiException::class.java) {
            runBlocking { service.createIssue(issueReq) }
        }
        assertTrue("Exception message should mention Jira API error", exception.message!!.contains("Jira API error") || exception.message!!.contains("Failed to create"))
    }

    @Test
    fun should_ApplyCorrectAuthHeader() = runBlocking {
        // --- TEST FLOW ---
        // 1. ARRANGE: Capture auth header.
        var capturedHeader: String? = null
        val service = createService { request ->
            capturedHeader = request.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        // 2. ACT: Execute connection test.
        service.testConnection("https://test.atlassian.net", "admin", "p@ssword")

        // 3. ASSERT: Verify Base64 encoding.
        assertEquals("Basic YWRtaW46cEBzc3dvcmQ=", capturedHeader)
    }
}