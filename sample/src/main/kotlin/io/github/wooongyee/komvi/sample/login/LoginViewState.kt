package io.github.wooongyee.komvi.sample.login

import android.os.Parcelable
import io.github.wooongyee.komvi.core.ViewState
import kotlinx.parcelize.Parcelize

/**
 * Login screen state
 */
@Parcelize
data class LoginViewState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState, Parcelable
