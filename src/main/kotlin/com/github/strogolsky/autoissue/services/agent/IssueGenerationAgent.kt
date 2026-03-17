package com.github.strogolsky.autoissue.services.agent

interface IssueGenerationAgent<I, O> {
    suspend fun generate(input: I): O?
}
