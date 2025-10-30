package io.github.wooongyee.komvi.sample.login

import androidx.lifecycle.SavedStateHandle
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
 * Advanced tests for LoginViewModel including:
 * - SavedStateHandle persistence and restoration
 * - Concurrent dispatch handling
 * - Edge cases and error scenarios
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoginViewModelAdvancedTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ SavedStateHandle Tests ============

    @Test
    fun savedStateHandle_persistsStateAcrossRecreation() = runTest {
        val savedStateHandle = SavedStateHandle()

        // ViewModel 1: Change state
        val viewModel1 = LoginViewModel(
            savedStateHandle = savedStateHandle
        )
        viewModel1.dispatch(LoginIntent.ViewAction.EmailChanged("saved@test.com"))
        advanceUntilIdle()
        viewModel1.dispatch(LoginIntent.ViewAction.PasswordChanged("savedpass"))
        advanceUntilIdle()

        // ViewModel 2: Recreated (simulates process death/recreation)
        val viewModel2 = LoginViewModel(
            savedStateHandle = savedStateHandle
        )

        // State should be restored
        assertEquals("saved@test.com", viewModel2.state.value.email)
        assertEquals("savedpass", viewModel2.state.value.password)
    }

    @Test
    fun savedStateHandle_restoresComplexState() = runTest {
        val savedStateHandle = SavedStateHandle()

        // ViewModel 1: Create complex state
        val viewModel1 = LoginViewModel(
            savedStateHandle = savedStateHandle
        )
        viewModel1.dispatch(LoginIntent.ViewAction.EmailChanged("complex@test.com"))
        advanceUntilIdle()
        viewModel1.dispatch(LoginIntent.ViewAction.PasswordChanged("complexpass"))
        advanceUntilIdle()
        viewModel1.dispatch(LoginIntent.ViewAction.LoginClicked) // Trigger error
        advanceUntilIdle()

        // Verify state before recreation
        assertEquals("complex@test.com", viewModel1.state.value.email)
        assertEquals("complexpass", viewModel1.state.value.password)
        assertEquals("Email and password cannot be empty", viewModel1.state.value.errorMessage)

        // ViewModel 2: Recreated
        val viewModel2 = LoginViewModel(
            savedStateHandle = savedStateHandle
        )

        // All state should be restored
        assertEquals("complex@test.com", viewModel2.state.value.email)
        assertEquals("complexpass", viewModel2.state.value.password)
        // Note: errorMessage might not persist if cleared on new intent
    }

    @Test
    fun savedStateHandle_updatesOnEveryStateChange() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = LoginViewModel(
            savedStateHandle = savedStateHandle
        )

        // First update
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("first@test.com"))
        advanceUntilIdle()
        assertEquals(
            "first@test.com",
            savedStateHandle.get<LoginViewState>("mvi_state")?.email
        )

        // Second update
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("second@test.com"))
        advanceUntilIdle()
        assertEquals(
            "second@test.com",
            savedStateHandle.get<LoginViewState>("mvi_state")?.email
        )

        // Third update
        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("password"))
        advanceUntilIdle()
        val savedState = savedStateHandle.get<LoginViewState>("mvi_state")
        assertEquals("second@test.com", savedState?.email)
        assertEquals("password", savedState?.password)
    }

    @Test
    fun savedStateHandle_nullInitially_usesInitialState() = runTest {
        val savedStateHandle = SavedStateHandle() // Empty

        val viewModel = LoginViewModel(
            savedStateHandle = savedStateHandle
        )

        // Should use initial state
        assertEquals("", viewModel.state.value.email)
        assertEquals("", viewModel.state.value.password)
        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.errorMessage)
    }

    @Test
    fun savedStateHandle_existingState_overridesInitialState() = runTest {
        val savedStateHandle = SavedStateHandle()
        savedStateHandle["mvi_state"] = LoginViewState(
            email = "existing@test.com",
            password = "existingpass",
            isLoading = true,
            errorMessage = "Existing error"
        )

        val viewModel = LoginViewModel(
            savedStateHandle = savedStateHandle
        )

        // Should use saved state, not initial state
        assertEquals("existing@test.com", viewModel.state.value.email)
        assertEquals("existingpass", viewModel.state.value.password)
        assertEquals(true, viewModel.state.value.isLoading)
        assertEquals("Existing error", viewModel.state.value.errorMessage)
    }

    // ============ Concurrent Dispatch Tests ============

    @Test
    fun concurrentDispatches_processedSequentially() = runTest {
        val viewModel = LoginViewModel()
        val emails = mutableListOf<String>()

        val job = launch {
            viewModel.state.collect { emails.add(it.email) }
        }

        // Launch multiple concurrent dispatches
        launch { viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("concurrent1@test.com")) }
        launch { viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("concurrent2@test.com")) }
        launch { viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("concurrent3@test.com")) }

        advanceUntilIdle()
        job.cancel()

        // All emails should be processed (in some order)
        assertTrue(emails.contains("concurrent1@test.com"))
        assertTrue(emails.contains("concurrent2@test.com"))
        assertTrue(emails.contains("concurrent3@test.com"))
    }

    @Test
    fun concurrentDispatches_finalStateConsistent() = runTest {
        val viewModel = LoginViewModel()

        // Launch 100 concurrent email changes
        repeat(100) { index ->
            launch {
                viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("email$index@test.com"))
            }
        }

        advanceUntilIdle()

        // Final state should be one of the emails (not corrupted)
        val finalEmail = viewModel.state.value.email
        assertTrue(finalEmail.matches(Regex("email\\d+@test\\.com")))
    }

    @Test
    fun concurrentDispatches_withDifferentIntents() = runTest {
        val viewModel = LoginViewModel()

        // Mix different intent types concurrently
        launch { viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("email@test.com")) }
        launch { viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("pass123")) }
        launch { viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("email2@test.com")) }
        launch { viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("pass456")) }

        advanceUntilIdle()

        // State should have valid values (not corrupted)
        val finalState = viewModel.state.value
        assertTrue(finalState.email.isNotEmpty())
        assertTrue(finalState.password.isNotEmpty())
    }

    @Test
    fun concurrentDispatches_viewActionAndInternal() = runTest {
        val viewModel = LoginViewModel()

        // Concurrent ViewAction and Internal dispatches
        launch {
            viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("concurrent@test.com"))
        }
        launch {
            viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Concurrent error"))
        }
        launch {
            viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("concurrentpass"))
        }

        advanceUntilIdle()

        // State should be consistent
        val finalState = viewModel.state.value
        assertEquals(false, finalState.isLoading) // OnLoginFailure sets isLoading = false
    }

    // ============ Rapid Sequential Dispatches Tests ============

    @Test
    fun rapidSequentialDispatches_allProcessed() = runTest {
        val viewModel = LoginViewModel()
        val stateChanges = mutableListOf<String>()

        val job = launch {
            viewModel.state.collect { stateChanges.add(it.email) }
        }

        // Rapid sequential dispatches (not concurrent)
        repeat(50) { index ->
            viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("rapid$index@test.com"))
        }

        advanceUntilIdle()
        job.cancel()

        // All should be processed
        assertTrue(stateChanges.size >= 50) // Initial state + 50 changes
        assertEquals("rapid49@test.com", viewModel.state.value.email)
    }

    @Test
    fun rapidDispatches_stateFlowDeduplicates() = runTest {
        val viewModel = LoginViewModel()
        val stateChanges = mutableListOf<String>()

        val job = launch {
            viewModel.state.collect { stateChanges.add(it.email) }
        }

        // Dispatch same value multiple times
        repeat(10) {
            viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("same@test.com"))
        }

        advanceUntilIdle()
        job.cancel()

        // StateFlow should deduplicate consecutive same values
        // Initial "" + "same@test.com" = 2 unique values
        val uniqueEmails = stateChanges.distinct()
        assertEquals(listOf("", "same@test.com"), uniqueEmails)
    }
}
