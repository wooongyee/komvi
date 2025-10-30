package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Orchestrates ViewModel processing pipeline.
 *
 * Pipeline:
 * 1. Collect handlers via ViewModelVisitor
 * 2. Extract Intent type via IntentTypeExtractor
 * 3. Validate handlers via HandlerValidator
 * 4. Generate dispatch code via DispatchCodeGenerator
 */
class ViewModelProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {
    private val intentExtractor = IntentTypeExtractor(logger)
    private val handlerValidator = HandlerValidator(logger)
    private val dispatchGenerator = DispatchCodeGenerator(codeGenerator, logger)

    fun process(resolver: Resolver): List<KSAnnotated> {
        val viewActionHandlers = resolver.getSymbolsWithAnnotation(ClassNames.VIEW_ACTION_HANDLER)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val internalHandlers = resolver.getSymbolsWithAnnotation(ClassNames.INTERNAL_HANDLER)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val allHandlers = viewActionHandlers + internalHandlers

        // Group by parent ViewModel class
        val functionsByClass = allHandlers.groupBy { it.parentDeclaration as? KSClassDeclaration }

        functionsByClass.forEach { (viewModelClass, functions) ->
            if (viewModelClass != null) {
                processViewModel(viewModelClass, functions)
            }
        }

        return emptyList()
    }

    private fun processViewModel(
        viewModelClass: KSClassDeclaration,
        functions: List<KSFunctionDeclaration>
    ) {
        val className = viewModelClass.simpleName.asString()
        logger.info("Processing ViewModel: $className with ${functions.size} handler(s)")

        // 1. Collect handlers via visitor
        val visitor = ViewModelVisitor()
        functions.forEach { it.accept(visitor, Unit) }

        if (visitor.handlers.isEmpty()) {
            logger.warn("No handlers found in $className")
            return
        }

        // 2. Extract Intent type
        val intentClass = intentExtractor.extract(viewModelClass)
        if (intentClass == null) {
            logger.error("Cannot extract Intent type for $className")
            return
        }

        // 3. Validate handlers
        val validationResult = handlerValidator.validate(visitor.handlers, intentClass)
        if (!validationResult.isValid) {
            logger.error("Validation failed for $className, skipping code generation")
            return
        }

        // 4. Generate dispatch functions
        dispatchGenerator.generate(viewModelClass, intentClass, validationResult.validHandlers)
    }
}
