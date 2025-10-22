package io.github.wooongyee.komvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState

/**
 * Collects side effects from MviContainer in a lifecycle-aware manner.
 *
 * Side effects are collected only when the lifecycle is at least STARTED.
 * The collection automatically stops when the lifecycle falls below STARTED.
 *
 * @param lifecycleState The minimum lifecycle state for collecting side effects (default: STARTED)
 * @param onSideEffect Callback invoked for each side effect
 */
@Composable
fun <S : ViewState, E : SideEffect> MviContainer<S, E>.collectSideEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onSideEffect: suspend (E) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            sideEffect.collect { effect ->
                onSideEffect(effect)
            }
        }
    }
}
