package com.github.wooongyee.komvi.processor.fixtures

import com.github.wooongyee.komvi.annotations.InternalHandler
import com.github.wooongyee.komvi.annotations.ViewActionHandler

/**
 * Valid ViewModel for happy path testing
 */
class ValidTestViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

    @ViewActionHandler
    internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler {
    }

    @ViewActionHandler(log = true)
    internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler {
    }

    @ViewActionHandler(log = true)
    internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler {
    }

    @InternalHandler
    internal fun handleLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler {
    }

    @InternalHandler
    private fun handleError(intent: FakeIntent.Internal.OnError) = handler {
    }
}

/**
 * ViewModel with public handler (should fail validation)
 */
class InvalidPublicHandlerViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

    @ViewActionHandler
    fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler {
        // Public handler - should fail
    }
}

/**
 * ViewModel with wrong parameter count (should fail validation)
 */
class InvalidParameterCountViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

    @ViewActionHandler
    internal fun handleNoParams() = handler {
        // No parameters - should fail
    }

    @ViewActionHandler
    internal fun handleTwoParams(intent: FakeIntent.ViewAction.Increment, extra: String) = handler {
        // Two parameters - should fail
    }
}

/**
 * ViewModel with wrong Intent type annotation (should fail validation)
 */
class InvalidIntentTypeViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

    @ViewActionHandler
    internal fun handleInternal(intent: FakeIntent.Internal.OnLoadComplete) = handler {
        // Using @ViewActionHandler with Internal intent - should fail
    }

    @InternalHandler
    internal fun handleViewAction(intent: FakeIntent.ViewAction.Increment) = handler {
        // Using @InternalHandler with ViewAction intent - should fail
    }
}

/**
 * ViewModel with missing handler (should fail validation)
 */
class MissingHandlerViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

    @ViewActionHandler
    internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler {
        // Only handles Increment, missing Decrement and SetValue
    }
}

/**
 * ViewModel with duplicate handlers (should fail validation)
 */
class DuplicateHandlerViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

    @ViewActionHandler
    internal fun handleIncrement1(intent: FakeIntent.ViewAction.Increment) = handler {
        // First handler for Increment
    }

    @ViewActionHandler
    internal fun handleIncrement2(intent: FakeIntent.ViewAction.Increment) = handler {
        // Duplicate handler for Increment - should fail
    }
}

/**
 * Simple Intent for minimal testing
 */
sealed interface SimpleIntent : com.github.wooongyee.komvi.core.Intent {
    sealed interface ViewAction : SimpleIntent {
        data object Click : ViewAction
    }
}

/**
 * Minimal ViewModel with single handler
 */
class MinimalViewModel : FakeMviViewModel<FakeViewState, SimpleIntent, FakeSideEffect>() {

    @ViewActionHandler
    internal fun handleClick(intent: SimpleIntent.ViewAction.Click) = handler {
        // Simple handler
    }
}
