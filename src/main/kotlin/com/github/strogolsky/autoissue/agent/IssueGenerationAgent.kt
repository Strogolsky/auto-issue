package com.github.strogolsky.autoissue.agent

interface IssueGenerationAgent<I, O> {
    suspend fun generate(input: I): O?
}
