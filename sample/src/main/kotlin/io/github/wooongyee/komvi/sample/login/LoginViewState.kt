package io.github.wooongyee.komvi.sample.login

import io.github.wooongyee.komvi.core.ViewState

/**
 * Login screen state
 */
data class LoginViewState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState
