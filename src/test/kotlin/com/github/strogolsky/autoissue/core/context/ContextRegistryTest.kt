package com.github.strogolsky.autoissue.core.context

import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.providers.ContextComponentProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextRegistryTest {

    private val env = mockk<ContextEnvironment>()

    @Test
    fun should_ReturnEmptyList_When_RegistryIsEmpty() = runTest {
        // 1. ARRANGE
        val registry = ContextRegistry(emptyList())

        // 2. ACT
        val result = registry.gatherAll(env)

        // 3. ASSERT
        assertTrue(result.isEmpty())
    }

    @Test
    fun should_ReturnComponent_When_ProviderIsRegistered() = runTest {
        // 1. ARRANGE
        val provider = mockk<ContextComponentProvider>()
        val component = mockk<ContextComponent>()
        coEvery { provider.provide(env) } returns component

        val registry = ContextRegistry(listOf(provider))

        // 2. ACT
        val result = registry.gatherAll(env)

        // 3. ASSERT
        assertEquals(1, result.size)
        assertEquals(component, result[0])
    }

    @Test
    fun should_IgnoreNull_When_ProviderReturnsNull() = runTest {
        // 1. ARRANGE
        val provider = mockk<ContextComponentProvider>()
        coEvery { provider.provide(env) } returns null

        val registry = ContextRegistry(listOf(provider))

        // 2. ACT
        val result = registry.gatherAll(env)

        // 3. ASSERT
        assertTrue(result.isEmpty())
    }

    @Test
    fun should_ReturnAllComponents_When_MultipleProvidersRegistered() = runTest {
        // 1. ARRANGE
        val provider1 = mockk<ContextComponentProvider>()
        val component1 = mockk<ContextComponent>()
        coEvery { provider1.provide(env) } returns component1

        val provider2 = mockk<ContextComponentProvider>()
        val component2 = mockk<ContextComponent>()
        coEvery { provider2.provide(env) } returns component2

        val registry = ContextRegistry(listOf(provider1, provider2))

        // 2. ACT
        val result = registry.gatherAll(env)

        // 3. ASSERT
        assertEquals(2, result.size)
        assertTrue(result.contains(component1))
        assertTrue(result.contains(component2))
    }

    @Test
    fun should_SkipProviderAndContinue_When_ProviderThrowsException() = runTest {
        // 1. ARRANGE
        val failingProvider = mockk<ContextComponentProvider>()
        coEvery { failingProvider.provide(env) } throws RuntimeException("Error reading file")

        val successfulProvider = mockk<ContextComponentProvider>()
        val component = mockk<ContextComponent>()
        coEvery { successfulProvider.provide(env) } returns component

        val registry = ContextRegistry(listOf(failingProvider, successfulProvider))

        // 2. ACT
        val result = registry.gatherAll(env)

        // 3. ASSERT
        assertEquals(1, result.size)
        assertEquals(component, result[0])
    }
}