package io.github.wooongyee.komvi.sample.login

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for SideEffect emission and collection behavior.
 *
 * These tests verify that:
 * - Side effects are emitted in the correct order
 * - Multiple collectors all receive side effects
 * - Late subscribers do NOT receive previous side effects (hot flow behavior)
 * - Side effects work correctly with async operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoginViewModelSideEffectTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Side Effect Emission Tests ============

    @Test
    fun sideEffects_emittedInCorrectOrder() = runTest {
        val viewModel = LoginViewModel()
        val effects = mutableListOf<LoginSideEffect>()

        val job = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        // Trigger successful login flow
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("test@example.com"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("password"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        job.cancel()

        // Should receive both side effects from OnLoginSuccess
        assertEquals(2, effects.size)
        assertTrue(effects[0] is LoginSideEffect.NavigateToHome)
        assertTrue(effects[1] is LoginSideEffect.ShowToast)
        assertEquals("Login successful!", (effects[1] as LoginSideEffect.ShowToast).message)
    }

    @Test
    fun sideEffects_errorFlow_emitsToast() = runTest {
        val viewModel = LoginViewModel()
        val effects = mutableListOf<LoginSideEffect>()

        val job = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        // Trigger error flow
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Invalid credentials"))
        advanceUntilIdle()

        job.cancel()

        // Should receive error toast
        assertEquals(1, effects.size)
        assertTrue(effects[0] is LoginSideEffect.ShowToast)
        assertEquals("Invalid credentials", (effects[0] as LoginSideEffect.ShowToast).message)
    }

    // ============ Multiple Collectors Tests ============

    @Test
    fun sideEffects_multipleCollectors_allReceive() = runTest {
        val viewModel = LoginViewModel()
        val effects1 = mutableListOf<LoginSideEffect>()
        val effects2 = mutableListOf<LoginSideEffect>()

        val job1 = launch {
            viewModel.sideEffect.collect { effects1.add(it) }
        }

        val job2 = launch {
            viewModel.sideEffect.collect { effects2.add(it) }
        }

        // Trigger side effect
        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        job1.cancel()
        job2.cancel()

        // Both collectors should receive the same effects
        assertEquals(2, effects1.size)
        assertEquals(2, effects2.size)
        assertEquals(effects1[0], effects2[0])
        assertEquals(effects1[1], effects2[1])
    }

    // ============ Late Subscriber Tests ============

    @Test
    fun sideEffects_lateSubscriber_doesNotReceivePrevious() = runTest {
        val viewModel = LoginViewModel()

        // Emit side effect BEFORE subscribing
        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        // Subscribe AFTER side effect was emitted
        val lateEffects = mutableListOf<LoginSideEffect>()
        val job = launch {
            viewModel.sideEffect.collect { lateEffects.add(it) }
        }

        advanceUntilIdle()
        job.cancel()

        // Should NOT receive previous side effects (hot flow behavior)
        assertEquals(0, lateEffects.size)
    }

    @Test
    fun sideEffects_lateSubscriber_receivesNewEffects() = runTest {
        val viewModel = LoginViewModel()

        // Emit side effect BEFORE subscribing
        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        // Subscribe AFTER first side effect
        val lateEffects = mutableListOf<LoginSideEffect>()
        val job = launch {
            viewModel.sideEffect.collect { lateEffects.add(it) }
        }

        // Emit NEW side effect AFTER subscribing
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("New error"))
        advanceUntilIdle()

        job.cancel()

        // Should receive only the NEW side effect
        assertEquals(1, lateEffects.size)
        assertTrue(lateEffects[0] is LoginSideEffect.ShowToast)
        assertEquals("New error", (lateEffects[0] as LoginSideEffect.ShowToast).message)
    }

    // ============ Multiple Side Effects Tests ============

    @Test
    fun sideEffects_multipleCalls_allEmitted() = runTest {
        val viewModel = LoginViewModel()
        val effects = mutableListOf<LoginSideEffect>()

        val job = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        // Trigger multiple side effects
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Error 1"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Error 2"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        job.cancel()

        // Should receive all side effects in order
        assertEquals(4, effects.size)

        assertTrue(effects[0] is LoginSideEffect.ShowToast)
        assertEquals("Error 1", (effects[0] as LoginSideEffect.ShowToast).message)

        assertTrue(effects[1] is LoginSideEffect.ShowToast)
        assertEquals("Error 2", (effects[1] as LoginSideEffect.ShowToast).message)

        assertTrue(effects[2] is LoginSideEffect.NavigateToHome)

        assertTrue(effects[3] is LoginSideEffect.ShowToast)
        assertEquals("Login successful!", (effects[3] as LoginSideEffect.ShowToast).message)
    }

    // ============ Async Flow Tests ============

    @Test
    fun sideEffects_asyncLoginFlow_emittedAfterCompletion() = runTest {
        val viewModel = LoginViewModel()
        val effects = mutableListOf<LoginSideEffect>()

        val job = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        // Start async login
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("test@example.com"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("password"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)

        // Side effects should not be emitted yet (async operation in progress)
        assertEquals(0, effects.size)

        // Wait for async operation to complete
        advanceUntilIdle()

        // Now side effects should be emitted
        assertEquals(2, effects.size)
        assertTrue(effects[0] is LoginSideEffect.NavigateToHome)
        assertTrue(effects[1] is LoginSideEffect.ShowToast)

        job.cancel()
    }

    // ============ Duplicate Side Effects Tests ============

    @Test
    fun sideEffects_duplicates_allEmitted() = runTest {
        val viewModel = LoginViewModel()
        val effects = mutableListOf<LoginSideEffect>()

        val job = launch {
            viewModel.sideEffect.collect { effects.add(it) }
        }

        // Emit same side effect multiple times
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Same error"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Same error"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Same error"))
        advanceUntilIdle()

        job.cancel()

        // All duplicates should be emitted (no deduplication)
        assertEquals(3, effects.size)
        effects.forEach {
            assertTrue(it is LoginSideEffect.ShowToast)
            assertEquals("Same error", (it as LoginSideEffect.ShowToast).message)
        }
    }
}
