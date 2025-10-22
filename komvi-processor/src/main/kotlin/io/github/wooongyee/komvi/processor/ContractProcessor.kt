package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Processes Contract (Intent sealed interface/class) to generate typealiases.
 *
 * Analyzes @ViewAction and @Internal annotations on Intent declarations
 * and generates corresponding State and Effect typealiases.
 */
class ContractProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun process(resolver: Resolver): List<KSAnnotated> {
        val intentInterface = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString("io.github.wooongyee.komvi.core.Intent")
        ) ?: return emptyList()

        // Find all classes that implement Intent
        val intentClasses = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.getAllSuperTypes().any { superType ->
                superType.declaration.qualifiedName?.asString() == "io.github.wooongyee.komvi.core.Intent"
            }}

        intentClasses.forEach { intentClass ->
            processIntentContract(intentClass)
        }

        return emptyList()
    }

    private fun processIntentContract(intentClass: KSClassDeclaration) {
        val packageName = intentClass.packageName.asString()
        val intentName = intentClass.simpleName.asString()

        // Generate typealias for State and Effect based on Intent name
        // e.g., LoginIntent -> LoginState, LoginEffect
        val baseName = intentName.removeSuffix("Intent")

        logger.info("Processing Intent contract: $intentName -> ${baseName}State, ${baseName}Effect")

        // TODO: Analyze @ViewAction and @Internal annotations
        // TODO: Generate validation code for @Internal intents
    }
}
