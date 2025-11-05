package com.github.wooongyee.komvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.wooongyee.komvi.core.Intent
import com.github.wooongyee.komvi.core.MviContainerHost
import com.github.wooongyee.komvi.core.SideEffect
import com.github.wooongyee.komvi.core.ViewState

/**
 * Collects side effects from MviContainerHost in a lifecycle-aware manner.
 *
 * Side effects are collected only when the lifecycle is at least STARTED.
 * The collection automatically stops when the lifecycle falls below STARTED.
 *
 * @param lifecycleState The minimum lifecycle state for collecting side effects (default: STARTED)
 * @param onSideEffect Callback invoked for each side effect
 */
@Composable
fun <S : ViewState, I : Intent, E : SideEffect> MviContainerHost<S, I, E>.collectSideEffect(
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
