package io.github.wooongyee.komvi.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for classes that host an [MviContainer].
 *
 * Typically implemented by ViewModel classes to provide convenient
 * access to state and side effects.
 *
 * @param S The type of [ViewState]
 * @param I The type of [Intent]
 * @param E The type of [SideEffect]
 */
interface MviContainerHost<S : ViewState, I : Intent, E : SideEffect> {

    /**
     * Current state as a hot [StateFlow].
     * Exposed to the View layer for observation.
     */
    val state: StateFlow<S>

    /**
     * Side effects as a hot [Flow].
     * Exposed to the View layer for one-time event consumption.
     */
    val sideEffect: Flow<E>
}
