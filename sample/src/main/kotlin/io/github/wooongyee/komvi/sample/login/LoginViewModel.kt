package io.github.wooongyee.komvi.sample.login

import io.github.wooongyee.komvi.android.MviViewModel
import io.github.wooongyee.komvi.annotations.ViewActionHandler
import io.github.wooongyee.komvi.annotations.InternalHandler
import kotlinx.coroutines.delay

class LoginViewModel : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>(
    initialState = LoginViewState(),
    debugMode = true
) {

    @ViewActionHandler(log = true)
    internal fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = intent {
        reduce { copy(email = intent.email, errorMessage = null) }
    }

    @ViewActionHandler(log = true)
    internal fun handlePasswordChanged(intent: LoginIntent.ViewAction.PasswordChanged) = intent {
        reduce { copy(password = intent.password, errorMessage = null) }
    }

    @ViewActionHandler(log = true, track = true, measurePerformance = true)
    internal fun handleLoginClicked(intent: LoginIntent.ViewAction.LoginClicked) = intent {
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
            dispatch(LoginIntent.Internal.OnLoginSuccess)
        } else {
            dispatch(LoginIntent.Internal.OnLoginFailure("Invalid credentials"))
        }
    }

    @InternalHandler
    internal fun handleOnLoginSuccess(intent: LoginIntent.Internal.OnLoginSuccess) = intent {
        reduce { copy(isLoading = false) }
        postSideEffect(LoginSideEffect.NavigateToHome)
        postSideEffect(LoginSideEffect.ShowToast("Login successful!"))
    }

    @InternalHandler
    internal fun handleOnLoginFailure(intent: LoginIntent.Internal.OnLoginFailure) = intent {
        reduce {
            copy(
                isLoading = false,
                errorMessage = intent.error
            )
        }
        postSideEffect(LoginSideEffect.ShowToast(intent.error))
    }
}
