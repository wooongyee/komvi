package io.github.wooongyee.komvi.sample.login

import io.github.wooongyee.komvi.core.Intent

/**
 * Login screen intents
 */
sealed interface LoginIntent : Intent {
    /**
     * Intents that can be dispatched from View layer
     */
    sealed interface ViewAction : LoginIntent {
        data class EmailChanged(val email: String) : ViewAction
        data class PasswordChanged(val password: String) : ViewAction
        data object LoginClicked : ViewAction
    }

    /**
     * Intents that can only be dispatched from ViewModel internally
     */
    sealed interface Internal : LoginIntent {
        data object OnLoginSuccess : Internal
        data class OnLoginFailure(val error: String) : Internal
        data class ValidateEmail(val email: String) : Internal
    }
}
