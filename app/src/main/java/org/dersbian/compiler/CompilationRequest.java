package org.dersbian.compiler;

import java.nio.file.Path;

/**
 * Immutable parameters of a compilation request, produced by the CLI layer and consumed by the
 * compilation engine.
 *
 * @param source input source file.
 * @param output desired output file.
 * @param optimizationLevel requested optimization level.
 * @param emitIntermediateCode if {@code true}, requests the production of the intermediate code
 *     (IR).
 * @param diagnostics if {@code true}, enables advanced diagnostics.
 */
@SuppressWarnings("PMD.LongVariable")
public record CompilationRequest(
        Path source,
        Path output,
        OptimizationLevel optimizationLevel,
        boolean emitIntermediateCode,
        boolean diagnostics) {}
