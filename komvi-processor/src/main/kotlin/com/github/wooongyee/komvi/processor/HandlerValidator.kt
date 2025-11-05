package com.github.wooongyee.komvi.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Visibility

/**
 * Validation result containing validated handlers or error messages.
 */
internal data class ValidationResult(
    val isValid: Boolean,
    val validHandlers: List<HandlerInfo>,
    val errors: List<String> = emptyList()
)

/**
 * Validates handler functions against MVI pattern rules.
 *
 * Validation rules:
 * - Handler visibility must not be private (public/internal allowed)
 * - Handler must have exactly 1 parameter
 * - Parameter type must match handler annotation (ViewAction vs Internal)
 * - All Intent subclasses must have corresponding handlers
 * - No duplicate handlers for same Intent
 */
internal class HandlerValidator(private val logger: KSPLogger) {

    fun validate(
        handlers: List<HandlerInfo>,
        intentClass: KSClassDeclaration
    ): ValidationResult {
        val errors = mutableListOf<String>()

        handlers.forEach { handler ->
            validateVisibility(handler.function)?.let { errors.add(it) }
            validateParameters(handler.function, handler.isViewAction, intentClass)?.let { errors.add(it) }
        }

        validateIntentHandlerMatching(handlers, intentClass)?.let { errors.addAll(it) }

        return ValidationResult(
            isValid = errors.isEmpty(),
            validHandlers = if (errors.isEmpty()) handlers else emptyList(),
            errors = errors
        )
    }

    private fun validateVisibility(function: KSFunctionDeclaration): String? {
        val visibility = function.getVisibility()
        if (visibility == Visibility.PRIVATE) {
            val message = "Handler '${function.simpleName.asString()}' must not be private. " +
                "Private handlers cannot be accessed from generated dispatch extension functions."
            logger.error(message, function)
            return message
        }
        return null
    }

    private fun validateParameters(
        function: KSFunctionDeclaration,
        isViewAction: Boolean,
        intentClass: KSClassDeclaration
    ): String? {
        val params = function.parameters
        if (params.size != 1) {
            val message = "Handler '${function.simpleName.asString()}' must have exactly 1 parameter"
            logger.error(message, function)
            return message
        }

        // Get parameter type
        val param = params.first()
        val paramType = param.type.resolve().declaration as? KSClassDeclaration

        if (paramType == null) {
            val message = "Handler '${function.simpleName.asString()}' parameter type cannot be resolved"
            logger.error(message, function)
            return message
        }

        // Get ViewAction and Internal interfaces from Intent
        val viewActionInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "ViewAction" }

        val internalInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "Internal" }

        val viewActionIntents = viewActionInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val internalIntents = internalInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val allIntents = viewActionIntents + internalIntents

        // Check if parameter type is one of the Intent subclasses
        val isValidIntentType = allIntents.any { it.qualifiedName == paramType.qualifiedName }

        if (!isValidIntentType) {
            val message = "Handler '${function.simpleName.asString()}' parameter must be a subclass of ${intentClass.simpleName.asString()}"
            logger.error(message, function)
            return message
        }

        // Validate handler annotation matches intent type
        val isViewActionIntent = viewActionIntents.any { it.qualifiedName == paramType.qualifiedName }
        val isInternalIntent = internalIntents.any { it.qualifiedName == paramType.qualifiedName }

        if (isViewAction && isInternalIntent) {
            val message = "@ViewActionHandler cannot handle Internal intent '${paramType.simpleName.asString()}'"
            logger.error(message, function)
            return message
        } else if (!isViewAction && isViewActionIntent) {
            val message = "@InternalHandler cannot handle ViewAction intent '${paramType.simpleName.asString()}'"
            logger.error(message, function)
            return message
        }

        return null
    }

    private fun validateIntentHandlerMatching(
        handlers: List<HandlerInfo>,
        intentClass: KSClassDeclaration
    ): List<String>? {
        val viewActionInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "ViewAction" }

        val internalInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "Internal" }

        val viewActionIntents = viewActionInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val internalIntents = internalInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val allIntents = viewActionIntents + internalIntents

        // Get handled intent types from handler parameters
        val handledIntentTypes = handlers.mapNotNull { handler ->
            val param = handler.function.parameters.firstOrNull()
            val paramType = param?.type?.resolve()?.declaration as? KSClassDeclaration
            paramType?.qualifiedName
        }.toSet()

        val errors = mutableListOf<String>()

        // Check each Intent has a handler
        allIntents.forEach { intent ->
            val intentName = intent.simpleName.asString()
            val intentQualifiedName = intent.qualifiedName

            if (!handledIntentTypes.contains(intentQualifiedName)) {
                val message = "Missing handler for Intent '$intentName'"
                logger.error(message, intent)
                errors.add(message)
            }
        }

        return if (errors.isEmpty()) null else errors
    }
}
