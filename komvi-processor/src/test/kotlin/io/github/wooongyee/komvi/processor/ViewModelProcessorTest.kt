package io.github.wooongyee.komvi.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.kspIncremental
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class ViewModelProcessorTest {

    @Test
    fun `should generate dispatch functions for valid ViewModel`() {
        val provider = KomviSymbolProcessorProvider()
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "ValidTestViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class TestViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

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
            // 0.6.0 API: symbolProcessorProviders 프로퍼티 사용 (자동으로 componentRegistrars에 등록됨)

            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += provider
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generatedFiles = compilation.kspSourcesDir
            .resolve("kotlin")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        assertTrue(generatedFiles.isNotEmpty(),
            "No files were generated. Generated files: ${generatedFiles.map { it.name }}")
    }

    @Test
    fun `should compile successfully with annotation attributes`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "AnnotatedViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class AnnotatedViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

                        @ViewActionHandler(log = true, track = true, measurePerformance = true)
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
            symbolProcessorProviders = mutableListOf(KomviSymbolProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `should handle minimal ViewModel with single handler`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "MinimalViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.core.Intent
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    sealed interface TestIntent : Intent {
                        sealed interface ViewAction : TestIntent {
                            data object Click : ViewAction
                        }
                    }

                    class MinimalViewModel : FakeMviViewModel<FakeViewState, TestIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleClick(intent: TestIntent.ViewAction.Click) = handler { }
                    }
                    """.trimIndent()
                )
            )
            symbolProcessorProviders = mutableListOf(KomviSymbolProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `should handle ViewModel with only ViewAction handlers`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "ViewActionOnlyViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.core.Intent
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    sealed interface TestIntent : Intent {
                        sealed interface ViewAction : TestIntent {
                            data object Action1 : ViewAction
                            data object Action2 : ViewAction
                        }
                    }

                    class ViewActionOnlyViewModel : FakeMviViewModel<FakeViewState, TestIntent, FakeSideEffect>() {

                        @ViewActionHandler
                        internal fun handleAction1(intent: TestIntent.ViewAction.Action1) = handler { }

                        @ViewActionHandler
                        internal fun handleAction2(intent: TestIntent.ViewAction.Action2) = handler { }
                    }
                    """.trimIndent()
                )
            )
            symbolProcessorProviders = mutableListOf(KomviSymbolProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `should handle ViewModel with only Internal handlers`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "InternalOnlyViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.core.Intent
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    sealed interface TestIntent : Intent {
                        sealed interface Internal : TestIntent {
                            data object Event1 : Internal
                            data object Event2 : Internal
                        }
                    }

                    class InternalOnlyViewModel : FakeMviViewModel<FakeViewState, TestIntent, FakeSideEffect>() {

                        @InternalHandler
                        internal fun handleEvent1(intent: TestIntent.Internal.Event1) = handler { }

                        @InternalHandler
                        internal fun handleEvent2(intent: TestIntent.Internal.Event2) = handler { }
                    }
                    """.trimIndent()
                )
            )
            symbolProcessorProviders = mutableListOf(KomviSymbolProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }
}
