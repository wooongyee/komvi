package com.github.wooongyee.komvi.core

/**
 * Marker interface for one-time events in the MVI pattern.
 * Represents side effects that should be consumed only once.
 *
 * Common use cases:
 * - Showing Toast messages
 * - Navigating to another screen
 * - Showing dialogs
 * - Triggering animations
 *
 * Implementations should be sealed interfaces or data classes.
 *
 * Example:
 * ```
 * sealed interface LoginEffect : SideEffect {
 *     data class ShowToast(val message: String) : LoginEffect
 *     data object NavigateToHome : LoginEffect
 *     data class ShowError(val error: Throwable) : LoginEffect
 * }
 * ```
 */
interface SideEffect
