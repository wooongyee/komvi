package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Processes ViewModel classes with @IntentHandler annotations.
 *
 * Responsibilities:
 * 1. Validate @IntentHandler functions are private/internal
 * 2. Validate all Contract Intents have corresponding handlers
 * 3. Generate dispatch function that calls handlers
 */
class ViewModelProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun process(resolver: Resolver): List<KSAnnotated> {
        val intentHandlerAnnotation = "io.github.wooongyee.komvi.annotations.IntentHandler"

        val annotatedFunctions = resolver.getSymbolsWithAnnotation(intentHandlerAnnotation)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        // Group functions by parent ViewModel class
        val functionsByClass = annotatedFunctions.groupBy { it.parentDeclaration as? KSClassDeclaration }

        functionsByClass.forEach { (viewModelClass, functions) ->
            if (viewModelClass != null) {
                processViewModel(resolver, viewModelClass, functions)
            }
        }

        return emptyList()
    }

    private fun processViewModel(
        resolver: Resolver,
        viewModelClass: KSClassDeclaration,
        handlers: List<KSFunctionDeclaration>
    ) {
        val className = viewModelClass.simpleName.asString()
        logger.info("Processing ViewModel: $className with ${handlers.size} @IntentHandler functions")

        // 1. Validate handlers are private/internal
        validateHandlerVisibility(handlers)

        // 2. Find associated Contract
        val contractClass = findAssociatedContract(resolver, viewModelClass)
        if (contractClass == null) {
            logger.warn("Cannot find Contract for ViewModel: $className")
            return
        }

        // 3. Validate all Intents have handlers
        validateAllIntentsHaveHandlers(contractClass, handlers)

        // 4. Generate dispatch function
        generateDispatchFunction(viewModelClass, contractClass, handlers)
    }

    private fun validateHandlerVisibility(handlers: List<KSFunctionDeclaration>) {
        handlers.forEach { handler ->
            val visibility = handler.getVisibility()
            if (visibility == Visibility.PUBLIC) {
                logger.error(
                    "@IntentHandler function '${handler.simpleName.asString()}' must be private or internal, not public",
                    handler
                )
            }
        }
    }

    private fun findAssociatedContract(
        resolver: Resolver,
        viewModelClass: KSClassDeclaration
    ): KSClassDeclaration? {
        // Find Contract by analyzing MviContainerHost<S, E> implementation
        // S extends ViewState, E extends SideEffect

        val containerHostType = viewModelClass.getAllSuperTypes().firstOrNull { superType ->
            superType.declaration.qualifiedName?.asString() == "io.github.wooongyee.komvi.core.MviContainerHost"
        }

        if (containerHostType == null) {
            logger.warn("ViewModel ${viewModelClass.simpleName.asString()} does not implement MviContainerHost")
            return null
        }

        // Get type arguments: MviContainerHost<LoginViewState, LoginSideEffect>
        val typeArguments = containerHostType.arguments
        if (typeArguments.size != 2) {
            logger.warn("MviContainerHost should have 2 type arguments")
            return null
        }

        // Get ViewState type (first type argument)
        val viewStateType = typeArguments[0].type?.resolve()
        if (viewStateType == null) {
            logger.warn("Cannot resolve ViewState type")
            return null
        }

        // ViewState type is usually a typealias like LoginViewState
        // We need to find the Contract that contains the actual State class
        val viewStateDeclaration = viewStateType.declaration as? KSClassDeclaration
        if (viewStateDeclaration == null) {
            logger.warn("ViewState is not a class declaration")
            return null
        }

        // The parent of State class is the Contract object/class
        val contractClass = viewStateDeclaration.parentDeclaration as? KSClassDeclaration

        if (contractClass == null) {
            logger.warn("Cannot find Contract parent for ${viewStateDeclaration.simpleName.asString()}")
        }

        return contractClass
    }

    private fun validateAllIntentsHaveHandlers(
        contractClass: KSClassDeclaration,
        handlers: List<KSFunctionDeclaration>
    ) {
        // Find Intent sealed class in Contract
        val intentClass = contractClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.modifiers.contains(Modifier.SEALED) }

        if (intentClass == null) {
            logger.warn("Cannot find Intent sealed class in ${contractClass.simpleName.asString()}")
            return
        }

        // Get all Intent subclasses
        val allIntents = intentClass.getSealedSubclasses().toList()
        val handlerNames = handlers.map { it.simpleName.asString() }.toSet()

        // Check each Intent has a handler
        allIntents.forEach { intent ->
            val intentName = intent.simpleName.asString()
            // Expected handler name: handleEmailChanged for EmailChanged intent
            val expectedHandlerName = "handle$intentName"

            if (!handlerNames.contains(expectedHandlerName)) {
                logger.error(
                    "Missing @IntentHandler for Intent '$intentName'. " +
                            "Expected function: $expectedHandlerName",
                    intent
                )
            }
        }
    }

    private fun generateDispatchFunction(
        viewModelClass: KSClassDeclaration,
        contractClass: KSClassDeclaration,
        handlers: List<KSFunctionDeclaration>
    ) {
        val packageName = viewModelClass.packageName.asString()
        val viewModelName = viewModelClass.simpleName.asString()
        val contractName = contractClass.simpleName.asString()
        val baseName = contractName.removeSuffix("Contract")

        // Find Intent class
        val intentClass = contractClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.modifiers.contains(Modifier.SEALED) }

        if (intentClass == null) {
            logger.warn("Cannot generate dispatch: no Intent class found")
            return
        }

        // Get @ViewAction and @Internal intents
        val viewActionIntents = mutableListOf<KSClassDeclaration>()
        val internalIntents = mutableListOf<KSClassDeclaration>()

        intentClass.getSealedSubclasses().forEach { subclass ->
            val hasViewAction = subclass.annotations.any {
                it.shortName.asString() == "ViewAction"
            }
            val hasInternal = subclass.annotations.any {
                it.shortName.asString() == "Internal"
            }

            when {
                hasViewAction -> viewActionIntents.add(subclass)
                hasInternal -> internalIntents.add(subclass)
            }
        }

        // Generate dispatch function
        val intentTypeName = ClassName(packageName, "${baseName}Intent")

        val dispatchFunction = FunSpec.builder("dispatch")
            .receiver(viewModelClass.toClassName())
            .addParameter(ParameterSpec.builder("intent", intentTypeName).build())
            .addKdoc("""
                Dispatches intents to appropriate @IntentHandler functions.

                Only @ViewAction intents can be dispatched from View layer.
                @Internal intents will result in a runtime error.
            """.trimIndent())
            .beginControlFlow("when (intent)")
            .apply {
                // ViewAction cases - call handler
                viewActionIntents.forEach { viewAction ->
                    val intentName = viewAction.simpleName.asString()
                    val handlerName = "handle$intentName"
                    addStatement("is ${baseName}Intent.$intentName -> $handlerName(intent)")
                }

                // Internal cases - throw error
                internalIntents.forEach { internal ->
                    val intentName = internal.simpleName.asString()
                    addStatement(
                        "is ${baseName}Intent.$intentName -> error(%S)",
                        "@Internal Intent $intentName cannot be dispatched from View"
                    )
                }
            }
            .endControlFlow()
            .build()

        val fileSpec = FileSpec.builder(packageName, "${viewModelName}_Dispatch")
            .addFileComment("Generated by Komvi KSP Processor")
            .addFileComment("DO NOT EDIT MANUALLY")
            .addFunction(dispatchFunction)
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, viewModelClass.containingFile!!))

        logger.info("Generated dispatch function for $viewModelName")
    }
}
