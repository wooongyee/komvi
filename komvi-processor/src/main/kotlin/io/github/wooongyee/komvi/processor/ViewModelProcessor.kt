package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Processes ViewModel classes with @IntentHandler annotations.
 *
 * Generates Intent Router code that dispatches intents to appropriate handlers
 * with optional logging and tracking.
 */
class ViewModelProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun process(resolver: Resolver): List<KSAnnotated> {
        val intentHandlerAnnotation = "io.github.wooongyee.komvi.annotations.IntentHandler"

        val annotatedFunctions = resolver.getSymbolsWithAnnotation(intentHandlerAnnotation)
            .filterIsInstance<KSFunctionDeclaration>()

        annotatedFunctions.forEach { function ->
            processIntentHandler(function)
        }

        return emptyList()
    }

    private fun processIntentHandler(function: KSFunctionDeclaration) {
        val functionName = function.simpleName.asString()
        val parentClass = function.parentDeclaration as? KSClassDeclaration

        logger.info("Processing @IntentHandler: ${parentClass?.simpleName?.asString()}.${functionName}")

        // TODO: Extract log/track options from annotation
        // TODO: Generate Intent Router code
        // TODO: Add logging/tracking wrapper code
    }
}
