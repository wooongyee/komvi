package com.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracts Intent type from ViewModel's MviViewModel superclass type arguments.
 *
 * Example: LoginViewModel : MviViewModel<LoginViewState, LoginIntent, LoginSideEffect>
 *                                                         ^^^^^^^^^^^
 *                                                         Extracts this
 */
internal class IntentTypeExtractor(private val logger: KSPLogger) {

    fun extract(viewModelClass: KSClassDeclaration): KSClassDeclaration? {
        val className = viewModelClass.simpleName.asString()

        // Find MviViewModel supertype by checking if it implements MviViewModelMarker
        val mviViewModelType = viewModelClass.superTypes.firstOrNull { superType ->
            val declaration = superType.resolve().declaration as? KSClassDeclaration
            declaration?.let { checkIfImplementsMarker(it) } ?: false
        }

        if (mviViewModelType == null) {
            val availableSuperTypes = viewModelClass.superTypes
                .map { it.resolve().declaration.simpleName.asString() }
                .joinToString(", ")
            logger.error(
                "Cannot extract Intent type for $className:\n" +
                "  $className does not extend MviViewModel (must implement MviViewModelMarker)\n" +
                "  Available super types: [$availableSuperTypes]"
            )
            return null
        }

        // Get type arguments: MviViewModel<S, I, E>
        val typeArguments = mviViewModelType.element?.typeArguments
        if (typeArguments == null || typeArguments.size != 3) {
            val actualCount = typeArguments?.size ?: 0
            logger.error(
                "Cannot extract Intent type for $className:\n" +
                "  MviViewModel should have 3 type arguments (ViewState, Intent, SideEffect)\n" +
                "  Found: $actualCount type argument(s)"
            )
            return null
        }

        // Get Intent type (second type argument, index 1)
        val intentType = typeArguments[1].type?.resolve()
        if (intentType == null) {
            logger.error(
                "Cannot extract Intent type for $className:\n" +
                "  Cannot resolve Intent type (second type argument)"
            )
            return null
        }

        val intentClass = intentType.declaration as? KSClassDeclaration
        if (intentClass == null) {
            logger.error(
                "Cannot extract Intent type for $className:\n" +
                "  Intent is not a class declaration (found: ${intentType.declaration})"
            )
            return null
        }

        logger.info("Extracted Intent type: ${intentClass.simpleName.asString()} from $className")
        return intentClass
    }

    /**
     * Recursively checks if a class declaration implements MviViewModelMarker
     */
    private fun checkIfImplementsMarker(declaration: KSClassDeclaration): Boolean {
        // Check if this class itself is the marker
        if (declaration.qualifiedName?.asString() == ClassNames.MVI_VIEW_MODEL_MARKER) {
            return true
        }

        // Check if this class directly implements the marker
        val implementsMarker = declaration.superTypes.any { superType ->
            superType.resolve().declaration.qualifiedName?.asString() == ClassNames.MVI_VIEW_MODEL_MARKER
        }

        if (implementsMarker) {
            return true
        }

        // Recursively check superclasses
        return declaration.superTypes.any { superType ->
            val superDeclaration = superType.resolve().declaration as? KSClassDeclaration
            superDeclaration?.let { checkIfImplementsMarker(it) } ?: false
        }
    }
}
