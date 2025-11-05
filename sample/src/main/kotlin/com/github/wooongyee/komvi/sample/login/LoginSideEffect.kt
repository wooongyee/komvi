package com.github.wooongyee.komvi.sample.login

import com.github.wooongyee.komvi.core.SideEffect

/**
 * Login screen side effects
 */
sealed interface LoginSideEffect : SideEffect {
    data object NavigateToHome : LoginSideEffect
    data class ShowToast(val message: String) : LoginSideEffect
}
