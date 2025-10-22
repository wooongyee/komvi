package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Generates validation code for Intent usage.
 *
 * Ensures @Internal intents cannot be dispatched from View layer,
 * and validates state transitions according to defined rules.
 */
class ValidationCodegen(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun process(resolver: Resolver): List<KSAnnotated> {
        val internalAnnotation = "io.github.wooongyee.komvi.annotations.Internal"

        val internalIntents = resolver.getSymbolsWithAnnotation(internalAnnotation)
            .filterIsInstance<KSClassDeclaration>()

        internalIntents.forEach { intent ->
            processInternalIntent(intent)
        }

        return emptyList()
    }

    private fun processInternalIntent(intent: KSClassDeclaration) {
        val intentName = intent.simpleName.asString()

        logger.info("Processing @Internal intent: $intentName")

        // TODO: Generate compile-time check to prevent View from dispatching @Internal intents
        // TODO: Add validation for state transition rules
    }
}
