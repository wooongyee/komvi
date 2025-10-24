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

        // 2. Find associated Intent
        val intentClass = findAssociatedIntent(resolver, viewModelClass)
        if (intentClass == null) {
            logger.warn("Cannot find Intent for ViewModel: $className")
            return
        }

        // 3. Validate all Intents have handlers
        validateAllIntentsHaveHandlers(intentClass, handlers)

        // 4. Generate dispatch function
        generateDispatchFunction(viewModelClass, intentClass, handlers)
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

    private fun findAssociatedIntent(
        resolver: Resolver,
        viewModelClass: KSClassDeclaration
    ): KSClassDeclaration? {
        // Find Intent by analyzing the superclass type arguments
        // e.g., LoginViewModel : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>

        // Get the direct superclass (MviViewModel)
        val superClass = viewModelClass.superTypes.firstOrNull { superType ->
            val declaration = superType.resolve().declaration
            declaration.qualifiedName?.asString()?.contains("MviViewModel") == true
        }

        if (superClass == null) {
            logger.warn("ViewModel ${viewModelClass.simpleName.asString()} does not extend MviViewModel")
            return null
        }

        // Get type arguments from superclass: MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>
        val typeArguments = superClass.element?.typeArguments
        if (typeArguments == null || typeArguments.size != 3) {
            logger.warn("MviViewModel should have 3 type arguments (ViewState, Intent, SideEffect)")
            return null
        }

        // Get Intent type (second type argument)
        val intentType = typeArguments[1].type?.resolve()
        if (intentType == null) {
            logger.warn("Cannot resolve Intent type")
            return null
        }

        logger.info("Intent type: ${intentType.declaration.qualifiedName?.asString()}")

        // Intent type is LoginIntent
        val intentClass = intentType.declaration as? KSClassDeclaration
        if (intentClass == null) {
            logger.warn("Intent is not a class declaration")
            return null
        }

        logger.info("Found Intent: ${intentClass.simpleName.asString()}")

        return intentClass
    }

    private fun validateAllIntentsHaveHandlers(
        intentClass: KSClassDeclaration,
        handlers: List<KSFunctionDeclaration>
    ) {
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
        intentClass: KSClassDeclaration,
        handlers: List<KSFunctionDeclaration>
    ) {
        val packageName = viewModelClass.packageName.asString()
        val viewModelName = viewModelClass.simpleName.asString()
        val intentName = intentClass.simpleName.asString()

        // Build handler metadata map (intent name -> (log, measurePerformance))
        val handlerMetadata = handlers.associate { handler ->
            val intentHandlerAnnotation = handler.annotations.first {
                it.shortName.asString() == "IntentHandler"
            }
            val log = intentHandlerAnnotation.arguments.find { it.name?.asString() == "log" }
                ?.value as? Boolean ?: false
            val measurePerformance = intentHandlerAnnotation.arguments.find { it.name?.asString() == "measurePerformance" }
                ?.value as? Boolean ?: false

            val handlerName = handler.simpleName.asString()
            val intentSubclassName = handlerName.removePrefix("handle")
            intentSubclassName to (log to measurePerformance)
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
        val intentTypeName = intentClass.toClassName()

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
                // ViewAction cases - call handler with logging/performance
                viewActionIntents.forEach { viewAction ->
                    val subclassName = viewAction.simpleName.asString()
                    val handlerName = "handle$subclassName"
                    val (log, measurePerformance) = handlerMetadata[subclassName] ?: (false to false)

                    beginControlFlow("is $intentName.$subclassName ->")

                    if (log) {
                        addStatement("android.util.Log.d(%S, %S + intent)", viewModelName, "Intent received: ")
                    }

                    if (measurePerformance) {
                        addStatement("val startTime = System.currentTimeMillis()")
                        addStatement("$handlerName(intent)")
                        addStatement("val duration = System.currentTimeMillis() - startTime")
                        addStatement("android.util.Log.d(%S, %S + duration + %S)", viewModelName, "Performance: $handlerName took ", "ms")
                    } else {
                        addStatement("$handlerName(intent)")
                    }

                    endControlFlow()
                }

                // Internal cases - throw error
                internalIntents.forEach { internal ->
                    val subclassName = internal.simpleName.asString()
                    addStatement(
                        "is $intentName.$subclassName -> error(%S)",
                        "@Internal Intent $subclassName cannot be dispatched from View"
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
