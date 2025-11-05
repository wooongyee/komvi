package io.github.wooongyee.komvi.sample.processor

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wooongyee.komvi.android.MviViewModel
import io.github.wooongyee.komvi.annotations.ViewActionHandler
import io.github.wooongyee.komvi.core.KomviLogger
import io.github.wooongyee.komvi.sample.login.LoginIntent
import io.github.wooongyee.komvi.sample.login.LoginSideEffect
import io.github.wooongyee.komvi.sample.login.LoginViewModel
import io.github.wooongyee.komvi.sample.login.LoginViewState
import io.github.wooongyee.komvi.sample.login.dispatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for KSP-generated code.
 *
 * These tests verify that the annotation processor correctly generates:
 * - dispatch() functions for ViewAction and Internal intents
 * - Proper handler invocations
 * - Annotation features (logging, performance measurement)
 *
 * ## Compile-time safety (enforced by type system and processor):
 *
 * **Type safety** - These would cause compilation errors:
 * ```
 * // ❌ View cannot dispatch Internal intents (type mismatch)
 * viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
 *
 * // ❌ intentScope is protected - cannot be called from outside
 * viewModel.intentScope { reduce { copy(email = "hack") } }
 *
 * // ❌ state is immutable - cannot be directly modified
 * viewModel.state.value.email = "hack"
 * ```
 *
 * **Processor validation** - These would cause processor errors:
 * ```
 * // ❌ @ViewActionHandler cannot handle Internal intent
 * @ViewActionHandler
 * fun handleOnLoginSuccess(intent: LoginIntent.Internal.OnLoginSuccess) = intentScope { ... }
 *
 * // ❌ @InternalHandler cannot handle ViewAction intent
 * @InternalHandler
 * fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = intentScope { ... }
 *
 * // ❌ Handler function must be private or internal, not public
 * @ViewActionHandler
 * public fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = intentScope { ... }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class GeneratedCodeIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Dispatch Function Generation Tests ============

    @Test
    fun viewActionDispatch_callsCorrectHandler() = runTest {
        val viewModel = LoginViewModel()

        // Dispatch ViewAction.EmailChanged
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("test@example.com"))
        advanceUntilIdle()

        // Should call handleEmailChanged and update state
        assertEquals("test@example.com", viewModel.state.value.email)
    }

    @Test
    fun viewActionDispatch_withMultipleActions() = runTest {
        val viewModel = LoginViewModel()

        // Dispatch multiple ViewActions
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("user@test.com"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("password123"))
        advanceUntilIdle()

        // Both handlers should be called
        assertEquals("user@test.com", viewModel.state.value.email)
        assertEquals("password123", viewModel.state.value.password)
    }

    @Test
    fun internalDispatch_callsCorrectHandler() = runTest {
        val viewModel = LoginViewModel()

        // Dispatch Internal intent
        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        // Should call handleOnLoginSuccess
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun internalDispatch_withError() = runTest {
        val viewModel = LoginViewModel()

        // Dispatch Internal.OnLoginFailure
        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("Invalid credentials"))
        advanceUntilIdle()

        // Should call handleOnLoginFailure and set error
        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals("Invalid credentials", viewModel.state.value.errorMessage)
    }

    // ============ Annotation Feature Tests ============

    @Test
    fun loggingAnnotation_worksWithAndroidLog() = runTest {
        val viewModel = LoginViewModel()

        // These actions have log=true
        // We can't easily verify android.util.Log in tests, but we verify the code compiles
        // and executes without errors
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("log@test.com"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("logpass"))
        advanceUntilIdle()

        // If logging causes any issues, these would fail
        assertEquals("log@test.com", viewModel.state.value.email)
        assertEquals("logpass", viewModel.state.value.password)
    }

    @Test
    fun logging_respectsDebugModeAndLogParameter() = runTest {
        // Test 1: debugMode = true (default, not explicitly passed), log = true → should log
        val mockLogger1 = MockLogger()
        val viewModel1 = LoginViewModel()
        // Use reflection to replace logger for testing
        val loggerField = viewModel1::class.java.getDeclaredField("logger")
        loggerField.isAccessible = true
        loggerField.set(viewModel1, mockLogger1)

        viewModel1.dispatch(LoginIntent.ViewAction.EmailChanged("test1@example.com"))
        advanceUntilIdle()

        assertTrue("Should log when debugMode=true (default) and log=true", mockLogger1.loggedMessages.isNotEmpty())
        assertTrue(mockLogger1.loggedMessages.any { it.contains("Intent received:") })
        assertEquals("test1@example.com", viewModel1.state.value.email)

        // Test 2: Verify LoginViewModel (without parameters) also uses default debugMode=true
        val viewModel2 = LoginViewModel()
        assertEquals("LoginViewModel should have debugMode=true by default", true, viewModel2.debugMode)
    }

    /**
     * Mock logger for testing logging behavior
     */
    private class MockLogger : KomviLogger {
        val loggedMessages = mutableListOf<String>()

        override fun debug(tag: String, message: String) {
            loggedMessages.add("[$tag] $message")
        }
    }

    @Test
    fun performanceMeasurement_worksWithoutErrors() = runTest {
        val viewModel = LoginViewModel()

        // LoginClicked has measurePerformance=true
        // We verify it executes without errors
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Should execute successfully (empty fields = error message)
        assertNotNull(viewModel.state.value.errorMessage)
    }

    // ============ Handler Coverage Tests ============

    @Test
    fun allViewActions_haveHandlers() = runTest {
        val viewModel = LoginViewModel()

        // Test all ViewAction intents can be dispatched
        // If a handler is missing, compilation would fail

        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("test"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("test"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // All should execute successfully
    }

    @Test
    fun allInternalIntents_haveHandlers() = runTest {
        val viewModel = LoginViewModel()

        // Test all Internal intents can be dispatched
        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("error"))
        advanceUntilIdle()

        // All should execute successfully
    }

    // ============ When Expression Coverage Tests ============

    @Test
    fun dispatchFunction_handlesAllBranches() = runTest {
        val viewModel = LoginViewModel()

        // The generated dispatch function should handle all sealed subclasses
        // This verifies the when expression is exhaustive

        // Test each ViewAction branch
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("branch1"))
        advanceUntilIdle()
        assertEquals("branch1", viewModel.state.value.email)

        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("branch2"))
        advanceUntilIdle()
        assertEquals("branch2", viewModel.state.value.password)

        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Test each Internal branch
        viewModel.dispatch(LoginIntent.Internal.OnLoginSuccess)
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.Internal.OnLoginFailure("branch4"))
        advanceUntilIdle()
        assertEquals("branch4", viewModel.state.value.errorMessage)
    }

    // ============ Complex Flow Tests ============

    @Test
    fun complexFlow_viewActionToInternalDispatch() = runTest {
        val viewModel = LoginViewModel()

        // ViewAction handler can dispatch Internal intents
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("test@example.com"))
        advanceUntilIdle()

        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("password"))
        advanceUntilIdle()

        // LoginClicked internally dispatches Internal.OnLoginSuccess or OnLoginFailure
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Wait for the async operation
        advanceUntilIdle()

        // Should complete successfully
        assertEquals(false, viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun complexFlow_errorHandling() = runTest {
        val viewModel = LoginViewModel()

        // Empty fields should trigger error
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Should have error message
        assertNotNull(viewModel.state.value.errorMessage)
        assertEquals("Email and password cannot be empty", viewModel.state.value.errorMessage)
    }
}
