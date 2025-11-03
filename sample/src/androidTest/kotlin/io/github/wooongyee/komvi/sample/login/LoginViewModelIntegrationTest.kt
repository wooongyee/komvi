package io.github.wooongyee.komvi.sample.login

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Business logic integration tests for LoginViewModel.
 *
 * These tests focus on the complete user flows and business logic,
 * not the implementation details of generated code.
 *
 * For KSP processor and generated code testing, see:
 * @see io.github.wooongyee.komvi.sample.processor.GeneratedCodeIntegrationTest
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoginViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Happy Path Tests ============

    @Test
    fun successfulLogin_withValidCredentials() = runTest {
        val viewModel = LoginViewModel()

        // Enter valid credentials
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("test@example.com"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("password"))
        advanceUntilIdle()

        // Click login
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Should complete successfully
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals("test@example.com", viewModel.state.value.email)
        assertEquals("password", viewModel.state.value.password)
    }

    // ============ Error Handling Tests ============

    @Test
    fun loginFails_withEmptyFields() = runTest {
        val viewModel = LoginViewModel()

        // Click login without entering credentials
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Should show validation error
        assertNotNull(viewModel.state.value.errorMessage)
        assertEquals("Email and password cannot be empty", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun loginFails_withInvalidCredentials() = runTest {
        val viewModel = LoginViewModel()

        // Enter invalid credentials
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("wrong@example.com"))
        advanceUntilIdle()
        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("wrongpass"))
        advanceUntilIdle()

        // Click login
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()

        // Should show error
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Invalid credentials", viewModel.state.value.errorMessage)
    }

    // ============ State Management Tests ============

    @Test
    fun emailChange_updatesStateAndClearsError() = runTest {
        val viewModel = LoginViewModel()

        // Trigger error first
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.errorMessage)

        // Change email should clear error
        viewModel.dispatch(LoginIntent.ViewAction.EmailChanged("new@example.com"))
        advanceUntilIdle()

        assertEquals("new@example.com", viewModel.state.value.email)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun passwordChange_updatesStateAndClearsError() = runTest {
        val viewModel = LoginViewModel()

        // Trigger error first
        viewModel.dispatch(LoginIntent.ViewAction.LoginClicked)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.errorMessage)

        // Change password should clear error
        viewModel.dispatch(LoginIntent.ViewAction.PasswordChanged("newpass"))
        advanceUntilIdle()

        assertEquals("newpass", viewModel.state.value.password)
        assertNull(viewModel.state.value.errorMessage)
    }
}
