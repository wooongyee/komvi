package io.github.wooongyee.komvi.sample.login

import io.github.wooongyee.komvi.core.SideEffect

/**
 * Login screen side effects
 */
sealed interface LoginSideEffect : SideEffect {
    data object NavigateToHome : LoginSideEffect
    data class ShowToast(val message: String) : LoginSideEffect
}
