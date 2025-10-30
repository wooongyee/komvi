package io.github.wooongyee.komvi.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspSourcesDir
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class CodeGenerationTest {

    @Test
    fun `generated file should have correct naming convention`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "MyViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class MyViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

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
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val kspGeneratedDir = compilation.kspSourcesDir.resolve("kotlin")
        val generatedFiles = kspGeneratedDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        val dispatchFile = generatedFiles.firstOrNull { it.name.contains("MyViewModel_Dispatch") }
        assertTrue(dispatchFile != null, "Expected MyViewModelDispatch.kt file. Found: ${generatedFiles.map { it.name }}")
    }

    @Test
    fun `generated dispatch functions should have correct signatures`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "TestViewModel.kt",
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
            configureKsp(useKsp2 = true) {
                symbolProcessorProviders += KomviSymbolProcessorProvider()
            }
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generatedCode = getGeneratedCode(compilation, "TestViewModel")

        // Check extension function format
        assertTrue(
            generatedCode.contains("fun test.TestViewModel.dispatch") ||
            generatedCode.contains("fun TestViewModel.dispatch"),
            "Generated code should have dispatch extension function"
        )
    }

    @Test
    fun `generated code should use when expression for dispatch`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "WhenTestViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class WhenTestViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

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
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generatedCode = getGeneratedCode(compilation, "WhenTestViewModel")

        // Check when expression structure
        assertTrue(
            generatedCode.contains("when (intent)") || generatedCode.contains("when(intent)"),
            "Generated code should use when expression"
        )
    }

    @Test
    fun `generated code should call executeHandler`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "HandlerCallViewModel.kt",
                    """
                    package test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class HandlerCallViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

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
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generatedCode = getGeneratedCode(compilation, "HandlerCallViewModel")

        // Check executeHandler calls
        assertTrue(generatedCode.contains("executeHandler"), "Should call executeHandler")
    }

    @Test
    fun `generated code should be in same package as ViewModel`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(
                    "PackageTestViewModel.kt",
                    """
                    package com.example.test

                    import io.github.wooongyee.komvi.annotations.ViewActionHandler
                    import io.github.wooongyee.komvi.annotations.InternalHandler
                    import io.github.wooongyee.komvi.processor.fixtures.*

                    class PackageTestViewModel : FakeMviViewModel<FakeViewState, FakeIntent, FakeSideEffect>() {

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
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        val generatedCode = getGeneratedCode(compilation, "PackageTestViewModel")

        assertTrue(generatedCode.contains("package com.example.test"), "Should be in same package")
    }

    private fun getGeneratedCode(compilation: KotlinCompilation, viewModelName: String): String {
        val kspGeneratedDir = compilation.kspSourcesDir.resolve("kotlin")
        val generatedFiles = kspGeneratedDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

        val dispatchFile = generatedFiles.firstOrNull { it.name.contains("${viewModelName}_Dispatch") }
        assertTrue(dispatchFile != null, "Dispatch file for $viewModelName was not generated. Found files: ${generatedFiles.map { it.name }}")

        return dispatchFile!!.readText()
    }
}
