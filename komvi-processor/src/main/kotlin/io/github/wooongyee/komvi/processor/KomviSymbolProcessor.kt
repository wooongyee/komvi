package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * KSP symbol processor for komvi annotations.
 *
 * Processes @ViewAction and @IntentHandler annotations to generate boilerplate code.
 */
class KomviSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // TODO: Implement annotation processing
        return emptyList()
    }
}
