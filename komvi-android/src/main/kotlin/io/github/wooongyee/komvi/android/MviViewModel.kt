package io.github.wooongyee.komvi.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wooongyee.komvi.core.Intent
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.MviContainerHost
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import io.github.wooongyee.komvi.core.container

/**
 * Base ViewModel class that integrates MVI pattern with Android's ViewModel lifecycle.
 *
 * This abstract class provides MVI container functionality tied to the ViewModel's lifecycle scope.
 * The container is automatically cleaned up when the ViewModel is cleared.
 *
 * @param S The type of view state that extends [ViewState]
 * @param I The type of intent that extends [Intent]
 * @param E The type of side effect that extends [SideEffect]
 * @param initialState The initial state of the view
 * @param debugMode Enable debug logging for state changes (default: false, use BuildConfig.DEBUG in production)
 */
abstract class MviViewModel<S : ViewState, I : Intent, E : SideEffect>(
    initialState: S,
    debugMode: Boolean = false
) : ViewModel(), MviContainerHost<S, I, E> {

    private val _container: MviContainer<S, I, E> = container(
        initialState = initialState,
        scope = viewModelScope,
        debugMode = debugMode
    )

    override val container: MviContainer<S, I, E>
        get() = throw UnsupportedOperationException("Direct container access is not allowed. Use intentScope in Intent handlers.")

    override val state get() = _container.state
    override val sideEffect get() = _container.sideEffect

    override fun intentScope(block: suspend io.github.wooongyee.komvi.core.IntentScope<S, I, E>.() -> Unit) {
        _container.intent(block)
    }
}
