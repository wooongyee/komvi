package io.github.wooongyee.komvi.processor

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
    val validHandlers: List<HandlerInfo>
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
        var hasError = false

        handlers.forEach { handler ->
            if (!validateVisibility(handler.function)) hasError = true
            if (!validateParameters(handler.function, handler.isViewAction, intentClass)) hasError = true
        }

        if (!validateIntentHandlerMatching(handlers, intentClass)) hasError = true

        return ValidationResult(
            isValid = !hasError,
            validHandlers = if (!hasError) handlers else emptyList()
        )
    }

    private fun validateVisibility(function: KSFunctionDeclaration): Boolean {
        val visibility = function.getVisibility()
        if (visibility == Visibility.PRIVATE) {
            logger.error(
                "Handler '${function.simpleName.asString()}' must not be private. " +
                "Private handlers cannot be accessed from generated dispatch extension functions.",
                function
            )
            return false
        }
        return true
    }

    private fun validateParameters(
        function: KSFunctionDeclaration,
        isViewAction: Boolean,
        intentClass: KSClassDeclaration
    ): Boolean {
        val params = function.parameters
        if (params.size != 1) {
            logger.error(
                "Handler '${function.simpleName.asString()}' must have exactly 1 parameter",
                function
            )
            return false
        }

        // Get parameter type
        val param = params.first()
        val paramType = param.type.resolve().declaration as? KSClassDeclaration

        if (paramType == null) {
            logger.error(
                "Handler '${function.simpleName.asString()}' parameter type cannot be resolved",
                function
            )
            return false
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
            logger.error(
                "Handler '${function.simpleName.asString()}' parameter must be a subclass of ${intentClass.simpleName.asString()}",
                function
            )
            return false
        }

        // Validate handler annotation matches intent type
        val isViewActionIntent = viewActionIntents.any { it.qualifiedName == paramType.qualifiedName }
        val isInternalIntent = internalIntents.any { it.qualifiedName == paramType.qualifiedName }

        if (isViewAction && isInternalIntent) {
            logger.error(
                "@ViewActionHandler cannot handle Internal intent '${paramType.simpleName.asString()}'",
                function
            )
            return false
        } else if (!isViewAction && isViewActionIntent) {
            logger.error(
                "@InternalHandler cannot handle ViewAction intent '${paramType.simpleName.asString()}'",
                function
            )
            return false
        }

        return true
    }

    private fun validateIntentHandlerMatching(
        handlers: List<HandlerInfo>,
        intentClass: KSClassDeclaration
    ): Boolean {
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

        var hasError = false

        // Check each Intent has a handler
        allIntents.forEach { intent ->
            val intentName = intent.simpleName.asString()
            val intentQualifiedName = intent.qualifiedName

            if (!handledIntentTypes.contains(intentQualifiedName)) {
                logger.error(
                    "Missing handler for Intent '$intentName'",
                    intent
                )
                hasError = true
            }
        }

        return !hasError
    }
}
