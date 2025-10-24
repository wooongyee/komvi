package io.github.wooongyee.komvi.sample.login

import io.github.wooongyee.komvi.android.MviViewModel
import io.github.wooongyee.komvi.annotations.IntentHandler
import kotlinx.coroutines.delay

class LoginViewModel : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>(
    initialState = LoginViewState(),
    debugMode = true
) {

    @IntentHandler(log = true)
    internal fun handleEmailChanged(intent: LoginIntent.EmailChanged) = intent {
        reduce { copy(email = intent.email, errorMessage = null) }
    }

    @IntentHandler(log = true)
    internal fun handlePasswordChanged(intent: LoginIntent.PasswordChanged) = intent {
        reduce { copy(password = intent.password, errorMessage = null) }
    }

    @IntentHandler(log = true, track = true, measurePerformance = true)
    internal fun handleLoginClicked(intent: LoginIntent.LoginClicked) = intent {
        val currentEmail = state.email
        val currentPassword = state.password

        if (currentEmail.isBlank() || currentPassword.isBlank()) {
            reduce { copy(errorMessage = "Email and password cannot be empty") }
            return@intent
        }

        reduce { copy(isLoading = true, errorMessage = null) }

        // Simulate API call
        delay(2000)

        if (currentEmail == "test@example.com" && currentPassword == "password") {
            handleOnLoginSuccess(LoginIntent.OnLoginSuccess)
        } else {
            handleOnLoginFailure(LoginIntent.OnLoginFailure("Invalid credentials"))
        }
    }

    @IntentHandler
    private fun handleOnLoginSuccess(intent: LoginIntent.OnLoginSuccess) = intent {
        reduce { copy(isLoading = false) }
        postSideEffect(LoginSideEffect.NavigateToHome)
        postSideEffect(LoginSideEffect.ShowToast("Login successful!"))
    }

    @IntentHandler
    private fun handleOnLoginFailure(intent: LoginIntent.OnLoginFailure) = intent {
        reduce {
            copy(
                isLoading = false,
                errorMessage = intent.error
            )
        }
        postSideEffect(LoginSideEffect.ShowToast(intent.error))
    }
}
