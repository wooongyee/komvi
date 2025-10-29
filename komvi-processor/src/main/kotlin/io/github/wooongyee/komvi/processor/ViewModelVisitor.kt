package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

/**
 * Handler function metadata collected during traversal.
 */
internal data class HandlerInfo(
    val function: KSFunctionDeclaration,
    val annotation: KSAnnotation,
    val isViewAction: Boolean
)

/**
 * Visitor that traverses ViewModel class to collect handler functions.
 * Finds all functions annotated with @ViewActionHandler or @InternalHandler.
 */
internal class ViewModelVisitor : KSVisitorVoid() {

    val handlers = mutableListOf<HandlerInfo>()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.declarations
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { it.accept(this, data) }
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        val viewActionAnnotation = function.annotations.firstOrNull {
            it.shortName.asString() == "ViewActionHandler"
        }
        val internalAnnotation = function.annotations.firstOrNull {
            it.shortName.asString() == "InternalHandler"
        }

        when {
            viewActionAnnotation != null -> {
                handlers.add(HandlerInfo(function, viewActionAnnotation, isViewAction = true))
            }
            internalAnnotation != null -> {
                handlers.add(HandlerInfo(function, internalAnnotation, isViewAction = false))
            }
        }
    }
}
