package com.github.wooongyee.komvi.sample.login

import android.os.Parcelable
import com.github.wooongyee.komvi.core.ViewState
import kotlinx.parcelize.Parcelize

/**
 * Login screen state
 */
@Parcelize
data class LoginViewState(
    val email: String = "",
    val password: String = "",
    val emailValidating: Boolean = false,
    val emailValid: Boolean? = null, // null: not validated, true: valid, false: invalid
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState, Parcelable
