package org.dersbian.compiler;

/**
 * Optimization levels available to the compiler.
 *
 * <p>The selected level is carried by {@link CompilationRequest} and can be used by the compiler
 * pipeline to control optimization work.
 */
public enum OptimizationLevel {
    /** Disables compiler optimizations. */
    NONE,
    /** Enables the basic optimization level. */
    BASIC,
    /** Enables the aggressive optimization level. */
    AGGRESSIVE
}
