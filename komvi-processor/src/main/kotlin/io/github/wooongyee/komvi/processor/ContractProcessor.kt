package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Processes Contract objects to generate typealiases and dispatch functions.
 *
 * Finds Contract objects (e.g., LoginContract) containing State, Intent, Effect
 * and generates:
 * 1. typealias LoginState = LoginContract.State
 * 2. typealias LoginIntent = LoginContract.Intent
 * 3. typealias LoginEffect = LoginContract.Effect
 * 4. dispatch function that only allows @ViewAction intents from View
 */
class ContractProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun process(resolver: Resolver): List<KSAnnotated> {
        // Find all classes/objects/interfaces that contain ViewState, Intent, SideEffect nested classes
        // (Contract pattern can be object, class, or interface)
        val contractObjects = resolver.getAllFiles()
            .flatMap { it.declarations }
            .filterIsInstance<KSClassDeclaration>()
            .filter { hasContractStructure(it) }

        contractObjects.forEach { contractObject ->
            processContract(contractObject)
        }

        return emptyList()
    }

    private fun hasContractStructure(classDecl: KSClassDeclaration): Boolean {
        val nestedClasses = classDecl.declarations
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        // Check for ViewState implementation
        val hasViewState = nestedClasses.any { nested ->
            nested.getAllSuperTypes().any { superType ->
                superType.declaration.qualifiedName?.asString() == "io.github.wooongyee.komvi.core.ViewState"
            }
        }

        // Check for Intent sealed interface/class
        val hasIntent = nestedClasses.any { nested ->
            (nested.classKind == ClassKind.INTERFACE || nested.classKind == ClassKind.CLASS) &&
                    nested.modifiers.contains(Modifier.SEALED)
        }

        // Check for SideEffect implementation
        val hasSideEffect = nestedClasses.any { nested ->
            nested.getAllSuperTypes().any { superType ->
                superType.declaration.qualifiedName?.asString() == "io.github.wooongyee.komvi.core.SideEffect"
            }
        }

        return hasViewState && hasIntent && hasSideEffect
    }

    private fun processContract(contractObject: KSClassDeclaration) {
        val contractName = contractObject.simpleName.asString() // e.g., "LoginContract"
        val baseName = contractName.removeSuffix("Contract") // e.g., "Login"
        val packageName = contractObject.packageName.asString()

        logger.info("Processing Contract: $contractName")

        // Find ViewState, Intent, SideEffect by interface implementation (not by name!)
        val nestedClasses = contractObject.declarations
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val stateClass = nestedClasses.firstOrNull { nested ->
            nested.getAllSuperTypes().any {
                it.declaration.qualifiedName?.asString() == "io.github.wooongyee.komvi.core.ViewState"
            }
        }

        val intentClass = nestedClasses.firstOrNull { nested ->
            nested.modifiers.contains(Modifier.SEALED)
        }

        val effectClass = nestedClasses.firstOrNull { nested ->
            nested.getAllSuperTypes().any {
                it.declaration.qualifiedName?.asString() == "io.github.wooongyee.komvi.core.SideEffect"
            }
        }

        if (stateClass == null || intentClass == null || effectClass == null) {
            logger.warn("Contract $contractName missing ViewState, Intent, or SideEffect implementation")
            return
        }

        logger.info("  Found: ${stateClass.simpleName.asString()} (ViewState), " +
                "${intentClass.simpleName.asString()} (Intent), " +
                "${effectClass.simpleName.asString()} (SideEffect)")


        // Analyze Intent subclasses for @ViewAction and @Internal
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

        logger.info("  @ViewAction intents: ${viewActionIntents.map { it.simpleName.asString() }}")
        logger.info("  @Internal intents: ${internalIntents.map { it.simpleName.asString() }}")

        // Generate only typealiases (dispatch will be generated by ViewModelProcessor)
        generateTypeAliases(contractObject, baseName, stateClass, intentClass, effectClass)
    }

    private fun generateTypeAliases(
        contractObject: KSClassDeclaration,
        baseName: String,
        stateClass: KSClassDeclaration,
        intentClass: KSClassDeclaration,
        effectClass: KSClassDeclaration
    ) {
        val packageName = contractObject.packageName.asString()
        val contractClassName = contractObject.toClassName()

        // Create typealiases using actual class names (not hardcoded "State", "Intent", "Effect")
        val stateAlias = TypeAliasSpec.builder(
            "${baseName}ViewState",
            contractClassName.nestedClass(stateClass.simpleName.asString())
        ).addKdoc("Type alias for ${baseName} view state").build()

        val intentAlias = TypeAliasSpec.builder(
            "${baseName}Intent",
            contractClassName.nestedClass(intentClass.simpleName.asString())
        ).addKdoc("Type alias for ${baseName} intent").build()

        val effectAlias = TypeAliasSpec.builder(
            "${baseName}SideEffect",
            contractClassName.nestedClass(effectClass.simpleName.asString())
        ).addKdoc("Type alias for ${baseName} side effect").build()

        val fileSpec = FileSpec.builder(packageName, "${baseName}Aliases")
            .addFileComment("Generated by Komvi KSP Processor")
            .addFileComment("DO NOT EDIT MANUALLY")
            .addTypeAlias(stateAlias)
            .addTypeAlias(intentAlias)
            .addTypeAlias(effectAlias)
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(false, contractObject.containingFile!!))

        logger.info("Generated typealiases: ${baseName}ViewState, ${baseName}Intent, ${baseName}SideEffect")
    }
}
