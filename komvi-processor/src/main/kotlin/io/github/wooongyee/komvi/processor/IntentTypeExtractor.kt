package io.github.wooongyee.komvi.processor

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
            logger.warn("$className does not extend MviViewModel (must implement MviViewModelMarker)")
            return null
        }

        // Get type arguments: MviViewModel<S, I, E>
        val typeArguments = mviViewModelType.element?.typeArguments
        if (typeArguments == null || typeArguments.size != 3) {
            logger.warn("MviViewModel should have 3 type arguments (ViewState, Intent, SideEffect)")
            return null
        }

        // Get Intent type (second type argument, index 1)
        val intentType = typeArguments[1].type?.resolve()
        if (intentType == null) {
            logger.warn("Cannot resolve Intent type for $className")
            return null
        }

        val intentClass = intentType.declaration as? KSClassDeclaration
        if (intentClass == null) {
            logger.warn("Intent is not a class declaration for $className")
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
