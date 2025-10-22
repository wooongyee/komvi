package io.github.wooongyee.komvi.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Generates validation code for Intent usage.
 *
 * Validation is primarily done in ContractProcessor's generated dispatch function.
 * This class can be extended for additional compile-time checks.
 */
class ValidationCodegen(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) {

    fun process(resolver: Resolver): List<KSAnnotated> {
        // Validation is now handled by the dispatch function in ContractProcessor
        // This processor is reserved for future validation extensions

        logger.info("Validation checks completed")

        return emptyList()
    }
}
