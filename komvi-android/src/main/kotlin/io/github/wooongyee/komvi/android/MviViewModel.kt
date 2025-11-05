package io.github.wooongyee.komvi.android

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wooongyee.komvi.annotations.InternalKomviApi
import io.github.wooongyee.komvi.core.Intent
import io.github.wooongyee.komvi.core.IntentScope
import io.github.wooongyee.komvi.core.KomviLogger
import io.github.wooongyee.komvi.core.MviContainer
import io.github.wooongyee.komvi.core.MviContainerHost
import io.github.wooongyee.komvi.core.MviViewModelMarker
import io.github.wooongyee.komvi.core.IntentExecutionStrategies
import io.github.wooongyee.komvi.core.IntentExecutionStrategy
import io.github.wooongyee.komvi.core.SideEffect
import io.github.wooongyee.komvi.core.ViewState
import io.github.wooongyee.komvi.core.container
import io.github.wooongyee.komvi.core.executeContainerIntent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that integrates MVI pattern with Android's ViewModel lifecycle.
 *
 * This abstract class provides MVI container functionality tied to the ViewModel's lifecycle scope.
 * The container is automatically cleaned up when the ViewModel is cleared.
 *
 * State changes can only happen through [intentScope] which is protected, preventing
 * direct manipulation from the View layer and enforcing the MVI pattern.
 *
 * Note: If using savedStateHandle, the state (S) must implement Parcelable.
 * Use @Parcelize annotation for easy implementation.
 *
 * @param S The type of view state that extends [ViewState]
 * @param I The type of intent that extends [Intent]
 * @param E The type of side effect that extends [SideEffect]
 * @param initialState The initial state of the view
 * @param savedStateHandle Optional SavedStateHandle for state persistence (requires S to be Parcelable)
 * @param stateKey Key for saving state in SavedStateHandle (default: "mvi_state")
 * @param debugMode Enable debug logging for intents (default: true). Set to false to disable all logging.
 * @param logger Logger implementation for intent logging (default: AndroidLogger)
 */
abstract class MviViewModel<S : ViewState, I : Intent, E : SideEffect>(
    initialState: S,
    savedStateHandle: SavedStateHandle? = null,
    stateKey: String = "mvi_state",
    val debugMode: Boolean = true,
    val logger: KomviLogger = AndroidLogger()
) : ViewModel(), MviContainerHost<S, I, E>, MviViewModelMarker {

    /**
     * Dispatcher for intent execution.
     *
     * **Production usage:**
     * Do NOT override in production code. The default (Dispatchers.Main.immediate) is appropriate
     * for most cases. Use withContext(Dispatchers.IO) or withContext(Dispatchers.Default)
     * inside handlers for IO/computation operations.
     *
     * **Test usage:**
     * Override this property in test ViewModels to inject TestDispatcher for deterministic testing.
     *
     * Example:
     * ```
     * // Production - use default (no override needed)
     * class MyViewModel : MviViewModel<S, I, E>(...)
     *
     * // Test - override dispatcher
     * class TestViewModel(
     *     private val testDispatcher: TestDispatcher
     * ) : MviViewModel<S, I, E>(...) {
     *     override val dispatcher = testDispatcher
     * }
     * ```
     */
    protected open val dispatcher: CoroutineDispatcher
        get() = Dispatchers.Main.immediate

    /**
     * Intent execution strategies for different coroutine execution modes.
     * Maps ExecutionMode enum names to their corresponding strategy implementations.
     */
    private val executionStrategies: Map<String, IntentExecutionStrategy> = mapOf(
        "CANCEL_PREVIOUS" to IntentExecutionStrategies.CancelPrevious(),
        "DROP" to IntentExecutionStrategies.Drop(),
        "QUEUE" to IntentExecutionStrategies.Queue(),
        "PARALLEL" to IntentExecutionStrategies.Parallel()
    )

    @PublishedApi
    internal val _container: MviContainer<S, I, E> by lazy {
        // Validate Parcelable requirement before creating container
        savedStateHandle?.let {
            require(initialState is Parcelable) {
                "State must implement Parcelable when using SavedStateHandle. " +
                "Use @Parcelize annotation on your ViewState data class."
            }
        }

        container<S, I, E>(
            initialState = savedStateHandle?.get<S>(stateKey) ?: initialState,
            scope = viewModelScope,
            debugMode = debugMode,
            dispatcher = dispatcher
        ).also { cont ->
            // Automatically save state when it changes
            savedStateHandle?.let { handle ->
                viewModelScope.launch {
                    cont.state.collect { currentState ->
                        handle[stateKey] = currentState
                    }
                }
            }
        }
    }

    override val state: StateFlow<S> get() = _container.state
    override val sideEffect: Flow<E> get() = _container.sideEffect

    /**
     * DSL helper function for defining intent handlers.
     * Wraps handler logic and returns it for KSP validation.
     *
     * Usage:
     * ```
     * @ViewActionHandler
     * internal fun handleSomeAction(intent: SomeIntent) = handler {
     *     reduce { copy(newState) }
     *     postSideEffect(SomeEffect)
     * }
     * ```
     *
     * @param block The intent processing block with [IntentScope] as receiver
     * @return The handler logic
     */
    protected fun handler(block: suspend IntentScope<S, I, E>.() -> Unit): suspend IntentScope<S, I, E>.() -> Unit = block

    /**
     * Executes an intent handler block within IntentScope.
     *
     * This function is called by KSP-generated dispatch functions.
     * Do not call this function directly from your application code.
     *
     * @param block The intent handler block to execute
     * @param executionMode ExecutionMode enum constant name (e.g., "PARALLEL", "DROP")
     * @param handlerKey Unique key identifying this handler (for job tracking)
     */
    @InternalKomviApi
    fun executeHandler(
        block: suspend IntentScope<S, I, E>.() -> Unit,
        executionMode: String = "PARALLEL",
        handlerKey: String
    ) {
        val strategy = executionStrategies[executionMode] ?: executionStrategies["PARALLEL"]!!
        strategy.execute(
            scope = viewModelScope,
            key = handlerKey
        ) {
            executeContainerIntent(_container, block)
        }
    }
}
