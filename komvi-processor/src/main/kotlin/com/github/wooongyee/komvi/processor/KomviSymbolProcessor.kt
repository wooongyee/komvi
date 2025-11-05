package com.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * KSP symbol processor for komvi annotations.
 *
 * Coordinates processing of @ViewActionHandler and @InternalHandler annotations
 * to generate dispatch functions.
 */
class KomviSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val viewModelProcessor = ViewModelProcessor(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Komvi annotation processing started")

        viewModelProcessor.process(resolver)

        logger.info("Komvi annotation processing completed")

        return emptyList()
    }
}
