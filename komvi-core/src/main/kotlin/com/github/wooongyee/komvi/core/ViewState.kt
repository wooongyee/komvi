package com.github.wooongyee.komvi.core

/**
 * Marker interface for UI state in the MVI pattern.
 * Represents the complete state of a screen or component.
 *
 * Implementations must be immutable data classes.
 *
 * Example:
 * ```
 * data class LoginState(
 *     val email: String = "",
 *     val password: String = "",
 *     val isLoading: Boolean = false,
 *     val error: String? = null
 * ) : ViewState
 * ```
 */
interface ViewState
