package com.github.wooongyee.komvi.annotations

/**
 * Marks declarations that are internal to Komvi framework.
 *
 * These APIs are intended for use by KSP-generated code only.
 * Using these APIs directly in application code is not recommended and may lead to undefined behavior.
 *
 * Calling code must opt-in using @OptIn(InternalKomviApi::class).
 */
@RequiresOptIn(
    message = "This is internal Komvi API. It should only be used by KSP-generated code. " +
            "Do not call this function directly from your application code.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class InternalKomviApi
