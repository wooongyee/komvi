package io.github.wooongyee.komvi.sample.login

import io.github.wooongyee.komvi.annotations.Internal
import io.github.wooongyee.komvi.annotations.ViewAction
import io.github.wooongyee.komvi.core.Intent

/**
 * Login screen intents
 */
sealed interface LoginIntent : Intent {
    @ViewAction
    data class EmailChanged(val email: String) : LoginIntent

    @ViewAction
    data class PasswordChanged(val password: String) : LoginIntent

    @ViewAction
    data object LoginClicked : LoginIntent

    @Internal
    data object OnLoginSuccess : LoginIntent

    @Internal
    data class OnLoginFailure(val error: String) : LoginIntent
}
