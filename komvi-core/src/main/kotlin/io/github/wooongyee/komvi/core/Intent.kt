package io.github.wooongyee.komvi.core

/**
 * Marker interface for all intents in the MVI pattern.
 * Represents all user intentions or actions that can occur in a View or ViewModel.
 *
 * Implementations should be immutable data classes or sealed interfaces.
 *
 * Example:
 * ```
 * sealed interface LoginIntent : Intent {
 *     data class EmailChanged(val email: String) : LoginIntent
 *     data class PasswordChanged(val password: String) : LoginIntent
 *     data object LoginClicked : LoginIntent
 * }
 * ```
 */
interface Intent
