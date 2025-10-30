package io.github.wooongyee.komvi.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class ValidationTest {

    // ========== Visibility Tests ==========

    @Test
    fun `should accept internal handler visibility`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "InternalVisibilityViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class InternalVisibilityViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
            "Internal handlers should be accepted. Got: ${result.messages}")
    }

    @Test
    fun `should accept public handler visibility`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "PublicVisibilityViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class PublicVisibilityViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
            "Public handlers should be accepted. Got: ${result.messages}")
    }

    @Test
    fun `should fail when handler is private`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "PrivateHandlerViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class PrivateHandlerViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        private fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("must not be private"),
            "Expected private handler error message. Got: ${result.messages}")
    }

    // ========== Parameter Validation Tests ==========

    @Test
    fun `should fail when handler has no parameters`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "NoParameterViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class NoParameterViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleIncrement() = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("must have exactly 1 parameter"),
            "Expected parameter count error. Got: ${result.messages}")
    }

    @Test
    fun `should fail when handler has multiple parameters`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "MultipleParametersViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class MultipleParametersViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment, extra: String) = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("must have exactly 1 parameter"),
            "Expected parameter count error. Got: ${result.messages}")
    }

    // ========== Annotation-Intent Matching Tests ==========

    @Test
    fun `should fail when ViewActionHandler annotates Internal intent handler`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "WrongAnnotationViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class WrongAnnotationViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @ViewActionHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("@ViewActionHandler") && result.messages.contains("cannot handle Internal intent"),
            "Expected ViewActionHandler/Internal mismatch error. Got: ${result.messages}")
    }

    @Test
    fun `should fail when InternalHandler annotates ViewAction intent handler`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "WrongInternalAnnotationViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class WrongInternalAnnotationViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @InternalHandler
                        internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun handleSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("@InternalHandler") && result.messages.contains("cannot handle ViewAction intent"),
            "Expected InternalHandler/ViewAction mismatch error. Got: ${result.messages}")
    }

    // ========== Missing Handler Tests ==========

    @Test
    fun `should fail when Intent has no corresponding handler`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "MissingHandlerViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class MissingHandlerViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        internal fun handleDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @InternalHandler
                        internal fun handleOnLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun handleOnError(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertTrue(result.messages.contains("Missing handler for Intent") && result.messages.contains("SetValue"),
            "Expected missing handler error for SetValue. Got: ${result.messages}")
    }

    @Test
    fun `should allow any function name for handlers`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "CustomNameViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class CustomNameViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun onIncrement(intent: FakeIntent.ViewAction.Increment) = handler { }

                        @ViewActionHandler
                        internal fun processDecrement(intent: FakeIntent.ViewAction.Decrement) = handler { }

                        @ViewActionHandler
                        internal fun customSetValue(intent: FakeIntent.ViewAction.SetValue) = handler { }

                        @InternalHandler
                        internal fun whenLoadComplete(intent: FakeIntent.Internal.OnLoadComplete) = handler { }

                        @InternalHandler
                        internal fun onErrorOccurred(intent: FakeIntent.Internal.OnError) = handler { }
                    }
                    """.trimIndent()
                )
            )
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode,
            "Custom function names should be accepted. Got: ${result.messages}")
    }
}
