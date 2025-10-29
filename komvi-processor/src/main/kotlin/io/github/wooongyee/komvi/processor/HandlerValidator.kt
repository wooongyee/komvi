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
 * - Handler visibility must be private or internal
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
        if (visibility == Visibility.PUBLIC) {
            logger.error(
                "@IntentHandler function '${function.simpleName.asString()}' must be private or internal, not public",
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

        // Get ViewAction and Internal interfaces from Intent
        val viewActionInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "ViewAction" }

        val internalInterface = intentClass.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull { it.simpleName.asString() == "Internal" }

        val viewActionIntents = viewActionInterface?.getSealedSubclasses()?.toList() ?: emptyList()
        val internalIntents = internalInterface?.getSealedSubclasses()?.toList() ?: emptyList()

        // Extract intent name from handler name: handleEmailChanged -> EmailChanged
        val handlerName = function.simpleName.asString()
        val intentName = handlerName.removePrefix("handle")

        // Find matching intent
        val matchingIntent = (viewActionIntents + internalIntents).find {
            it.simpleName.asString() == intentName
        }

        if (matchingIntent == null) {
            logger.error(
                "Handler '$handlerName' does not match any Intent subclass",
                function
            )
            return false
        }

        // Validate handler annotation matches intent type
        val isViewActionIntent = viewActionIntents.contains(matchingIntent)
        val isInternalIntent = internalIntents.contains(matchingIntent)

        if (isViewAction && isInternalIntent) {
            logger.error(
                "@ViewActionHandler function '$handlerName' cannot handle Internal intent '$intentName'",
                function
            )
            return false
        } else if (!isViewAction && isViewActionIntent) {
            logger.error(
                "@InternalHandler function '$handlerName' cannot handle ViewAction intent '$intentName'",
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

        val handlerNames = handlers.map { it.function.simpleName.asString() }.toSet()

        var hasError = false

        // Check each Intent has a handler
        allIntents.forEach { intent ->
            val intentName = intent.simpleName.asString()
            val expectedHandlerName = "handle$intentName"

            if (!handlerNames.contains(expectedHandlerName)) {
                logger.error(
                    "Missing handler for Intent '$intentName'. Expected function: $expectedHandlerName",
                    intent
                )
                hasError = true
            }
        }

        return !hasError
    }
}
