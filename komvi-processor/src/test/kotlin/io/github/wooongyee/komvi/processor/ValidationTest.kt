package io.github.wooongyee.komvi.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class ValidationTest {

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
}
