package io.github.wooongyee.komvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState

/**
 * Collects values from MviContainer's state and represents it via [State] in a lifecycle-aware manner.
 *
 * The state flow collection is tied to the lifecycle of the composable,
 * automatically starting and stopping collection based on lifecycle events.
 *
 * @return A [State] object representing the current view state
 */
@Composable
fun <S : ViewState, E : SideEffect> MviContainer<S, E>.collectAsStateWithLifecycle(): State<S> {
    return state.collectAsStateWithLifecycle()
}
