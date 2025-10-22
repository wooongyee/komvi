package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * KSP symbol processor for komvi annotations.
 *
 * Coordinates processing of @ViewAction, @Internal, and @IntentHandler annotations
 * to generate typealias, Intent routers, and validation code.
 */
class KomviSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val contractProcessor = ContractProcessor(codeGenerator, logger)
    private val viewModelProcessor = ViewModelProcessor(codeGenerator, logger)
    private val validationCodegen = ValidationCodegen(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Komvi annotation processing started")

        // Process Contract (Intent) declarations - generates typealiases
        contractProcessor.process(resolver)

        // Process ViewModel @IntentHandler functions - generates Intent routers
        viewModelProcessor.process(resolver)

        // Generate validation code for @Internal intents
        validationCodegen.process(resolver)

        logger.info("Komvi annotation processing completed")

        return emptyList()
    }
}
