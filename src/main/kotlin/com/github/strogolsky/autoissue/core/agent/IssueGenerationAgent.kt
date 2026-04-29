package com.github.strogolsky.autoissue.core.agent

interface IssueGenerationAgent<I, O> {
    suspend fun generate(input: I): O?
}
