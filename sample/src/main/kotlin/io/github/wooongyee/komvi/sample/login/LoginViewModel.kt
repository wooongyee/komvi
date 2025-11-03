package io.github.wooongyee.komvi.sample.login

import androidx.lifecycle.SavedStateHandle
import io.github.wooongyee.komvi.android.MviViewModel
import io.github.wooongyee.komvi.annotations.ExecutionMode
import io.github.wooongyee.komvi.annotations.ViewActionHandler
import io.github.wooongyee.komvi.annotations.InternalHandler
import kotlinx.coroutines.delay

class LoginViewModel(
    savedStateHandle: SavedStateHandle? = null
) : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>(
    initialState = LoginViewState(),
    savedStateHandle = savedStateHandle,
    debugMode = true
) {

    @ViewActionHandler(debug = true)
    internal fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = handler {
        reduce { copy(email = intent.email, emailValid = null, emailValidating = false, errorMessage = null) }
        if (intent.email.isNotBlank()) {
            dispatch(LoginIntent.Internal.ValidateEmail(intent.email))
        }
    }

    @ViewActionHandler(debug = true)
    internal fun handlePasswordChanged(intent: LoginIntent.ViewAction.PasswordChanged) = handler {
        reduce { copy(password = intent.password, errorMessage = null) }
    }

    // CANCEL_PREVIOUS: Debounce email validation to avoid redundant server calls
    @InternalHandler(debug = true, executionMode = ExecutionMode.CANCEL_PREVIOUS)
    internal fun handleValidateEmail(intent: LoginIntent.Internal.ValidateEmail) = handler {
        reduce { copy(emailValidating = true) }
        delay(300) // Simulate server API call
        val isValid = !intent.email.contains("test") // Simulate: "test" emails already exist
        reduce { copy(emailValidating = false, emailValid = isValid) }
    }

    // DROP: Prevent duplicate login attempts
    @ViewActionHandler(debug = true, executionMode = ExecutionMode.DROP)
    internal fun handleLoginClicked(intent: LoginIntent.ViewAction.LoginClicked) = handler {
        val currentEmail = state.email
        val currentPassword = state.password

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            reduce { copy(errorMessage = "Email and password cannot be empty") }
            return@handler
        }

        reduce { copy(isLoading = true, errorMessage = null) }

        // Simulate API call
        delay(2000)

        if (currentEmail == "test@example.com" && currentPassword == "password") {
            dispatch(LoginIntent.Internal.OnLoginSuccess)
        } else {
            dispatch(LoginIntent.Internal.OnLoginFailure("Invalid credentials"))
        }
    }

    @InternalHandler
    internal fun handleOnLoginSuccess(intent: LoginIntent.Internal.OnLoginSuccess) = handler {
        reduce { copy(isLoading = false) }
        postSideEffect(LoginSideEffect.NavigateToHome)
        postSideEffect(LoginSideEffect.ShowToast("Login successful!"))
    }

    @InternalHandler
    internal fun handleOnLoginFailure(intent: LoginIntent.Internal.OnLoginFailure) = handler {
        reduce {
            copy(
                isLoading = false,
                errorMessage = intent.error
            )
        }
        postSideEffect(LoginSideEffect.ShowToast(intent.error))
    }
}
