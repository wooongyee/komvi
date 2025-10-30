package io.github.wooongyee.komvi.sample.login

import androidx.lifecycle.SavedStateHandle
import io.github.wooongyee.komvi.android.MviViewModel
import io.github.wooongyee.komvi.annotations.ViewActionHandler
import io.github.wooongyee.komvi.annotations.InternalHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

class LoginViewModel(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    savedStateHandle: SavedStateHandle? = null
) : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>(
    initialState = LoginViewState(),
    savedStateHandle = savedStateHandle,
    debugMode = true,
    dispatcher = dispatcher
) {

    @ViewActionHandler(debug = true)
    internal fun handleEmailChanged(intent: LoginIntent.ViewAction.EmailChanged) = handler {
        reduce { copy(email = intent.email, errorMessage = null) }
    }

    @ViewActionHandler(debug = true)
    internal fun handlePasswordChanged(intent: LoginIntent.ViewAction.PasswordChanged) = handler {
        reduce { copy(password = intent.password, errorMessage = null) }
    }

    @ViewActionHandler(debug = true)
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
