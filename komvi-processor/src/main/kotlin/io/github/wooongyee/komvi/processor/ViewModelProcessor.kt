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
        val viewActionHandlerAnnotation = "io.github.wooongyee.komvi.annotations.ViewActionHandler"
        val internalHandlerAnnotation = "io.github.wooongyee.komvi.annotations.InternalHandler"

        val viewActionHandlers = resolver.getSymbolsWithAnnotation(viewActionHandlerAnnotation)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val internalHandlers = resolver.getSymbolsWithAnnotation(internalHandlerAnnotation)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        // Combine all handlers
        val allHandlers = viewActionHandlers + internalHandlers

        // Group functions by parent ViewModel class
        val functionsByClass = allHandlers.groupBy { it.parentDeclaration as? KSClassDeclaration }

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
        // Get ViewAction and Internal nested interfaces
        val viewActionInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "ViewAction" }

        val internalInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "Internal" }

        // Get actual intent subclasses (not the interfaces themselves)
        val viewActionIntents = viewActionInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val internalIntents = internalInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val allIntents = viewActionIntents + internalIntents

        val handlerNames = handlers.map { it.simpleName.asString() }.toSet()

        // Check each Intent has a handler
        allIntents.forEach { intent ->
            val intentName = intent.simpleName.asString()
            // Expected handler name: handleEmailChanged for EmailChanged intent
            val expectedHandlerName = "handle$intentName"

            if (!handlerNames.contains(expectedHandlerName)) {
                logger.error(
                    "Missing handler for Intent '$intentName'. " +
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

        // Build handler metadata map (intent name -> (log, measurePerformance, isViewAction))
        val handlerMetadata = handlers.associate { handler ->
            val viewActionAnnotation = handler.annotations.firstOrNull {
                it.shortName.asString() == "ViewActionHandler"
            }
            val internalAnnotation = handler.annotations.firstOrNull {
                it.shortName.asString() == "InternalHandler"
            }

            val annotation = viewActionAnnotation ?: internalAnnotation
            val isViewActionHandler = viewActionAnnotation != null

            val log = annotation?.arguments?.find { it.name?.asString() == "log" }
                ?.value as? Boolean ?: false
            val measurePerformance = annotation?.arguments?.find { it.name?.asString() == "measurePerformance" }
                ?.value as? Boolean ?: false

            val handlerName = handler.simpleName.asString()
            val intentSubclassName = handlerName.removePrefix("handle")
            intentSubclassName to Triple(log, measurePerformance, isViewActionHandler)
        }

        // Get ViewAction and Internal nested sealed interfaces
        val viewActionInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "ViewAction" }

        val internalInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "Internal" }

        val viewActionIntents = viewActionInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val internalIntents = internalInterface?.getSealedSubclasses()?.toList() ?: emptyList()

        // Validate handler-intent matching
        handlerMetadata.forEach { (intentSubclassName, metadata) ->
            val (_, _, isViewActionHandler) = metadata

            val intentClass = (viewActionIntents + internalIntents).find {
                it.simpleName.asString() == intentSubclassName
            }

            if (intentClass != null) {
                val isViewActionIntent = viewActionIntents.contains(intentClass)
                val isInternalIntent = internalIntents.contains(intentClass)

                if (isViewActionHandler && isInternalIntent) {
                    logger.error(
                        "@ViewActionHandler function 'handle$intentSubclassName' cannot handle @Internal intent '$intentSubclassName'",
                        handlers.find { it.simpleName.asString() == "handle$intentSubclassName" }
                    )
                } else if (!isViewActionHandler && isViewActionIntent) {
                    logger.error(
                        "@InternalHandler function 'handle$intentSubclassName' cannot handle @ViewAction intent '$intentSubclassName'",
                        handlers.find { it.simpleName.asString() == "handle$intentSubclassName" }
                    )
                }
            }
        }

        // Generate dispatch functions (ViewAction and Internal overloads)
        val viewModelClassName = viewModelClass.toClassName()
        val viewActionTypeName = ClassName(packageName, "$intentName.ViewAction")
        val internalTypeName = ClassName(packageName, "$intentName.Internal")

        // ViewAction dispatch function
        val viewActionDispatch = FunSpec.builder("dispatch")
            .receiver(viewModelClassName)
            .addParameter(ParameterSpec.builder("intent", viewActionTypeName).build())
            .addKdoc("Dispatches ViewAction intents from View layer")
            .beginControlFlow("when (intent)")
            .apply {
                viewActionIntents.forEach { viewAction ->
                    val subclassName = viewAction.simpleName.asString()
                    val handlerName = "handle$subclassName"
                    val (log, measurePerformance, _) = handlerMetadata[subclassName] ?: Triple(false, false, true)

                    beginControlFlow("is $intentName.ViewAction.$subclassName ->")

                    if (log) {
                        addStatement("android.util.Log.d(%S, %S + intent)", viewModelName, "Intent received: ")
                    }

                    if (measurePerformance) {
                        addStatement("val startTime = System.currentTimeMillis()")
                        addStatement("val logicBlock = $handlerName(intent)")
                        addStatement("this.executeHandler(logicBlock)")
                        addStatement("val duration = System.currentTimeMillis() - startTime")
                        addStatement("android.util.Log.d(%S, %S + duration + %S)", viewModelName, "Performance: $handlerName took ", "ms")
                    } else {
                        addStatement("val logicBlock = $handlerName(intent)")
                        addStatement("this.executeHandler(logicBlock)")
                    }

                    endControlFlow()
                }
            }
            .endControlFlow()
            .build()

        // Internal dispatch function
        val internalDispatch = FunSpec.builder("dispatch")
            .receiver(viewModelClassName)
            .addParameter(ParameterSpec.builder("intent", internalTypeName).build())
            .addKdoc("Dispatches Internal intents within ViewModel")
            .beginControlFlow("when (intent)")
            .apply {
                internalIntents.forEach { internal ->
                    val subclassName = internal.simpleName.asString()
                    val handlerName = "handle$subclassName"
                    val (log, measurePerformance, _) = handlerMetadata[subclassName] ?: Triple(false, false, false)

                    beginControlFlow("is $intentName.Internal.$subclassName ->")

                    if (log) {
                        addStatement("android.util.Log.d(%S, %S + intent)", viewModelName, "Intent received: ")
                    }

                    if (measurePerformance) {
                        addStatement("val startTime = System.currentTimeMillis()")
                        addStatement("val logicBlock = $handlerName(intent)")
                        addStatement("this.executeHandler(logicBlock)")
                        addStatement("val duration = System.currentTimeMillis() - startTime")
                        addStatement("android.util.Log.d(%S, %S + duration + %S)", viewModelName, "Performance: $handlerName took ", "ms")
                    } else {
                        addStatement("val logicBlock = $handlerName(intent)")
                        addStatement("this.executeHandler(logicBlock)")
                    }

                    endControlFlow()
                }
            }
            .endControlFlow()
            .build()

        val fileSpec = FileSpec.builder(packageName, "${viewModelName}_Dispatch")
            .addFileComment("Generated by Komvi KSP Processor")
            .addFileComment("DO NOT EDIT MANUALLY")
            .addFunction(viewActionDispatch)
            .addFunction(internalDispatch)
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, viewModelClass.containingFile!!))

        logger.info("Generated dispatch functions for $viewModelName")
    }
}
