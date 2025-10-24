package io.github.wooongyee.komvi.annotations

/**
 * Marks an Intent as a view action that can be dispatched from the View layer.
 *
 * Intents annotated with @ViewAction can be called from UI components.
 * The generated dispatch function will allow these intents to be processed.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewAction
