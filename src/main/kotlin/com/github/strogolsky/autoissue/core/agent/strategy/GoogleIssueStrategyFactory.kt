package com.github.strogolsky.autoissue.core.agent.strategy

abstract class GoogleIssueStrategyFactory<I, O> : IssueStrategyFactory<I, O> {
    override val providerKey = "GOOGLE"
}
