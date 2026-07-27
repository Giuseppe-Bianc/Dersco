package org.dersbian.compiler.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CompilerPhase}.
 *
 * <p>Coverage:
 *
 * <ul>
 *   <li>Constant count and declared identity of every value.
 *   <li>{@code name()} vs {@code ordinal()} invariants.
 *   <li>{@code valueOf} round-trip and null/empty/invalid argument behavior.
 *   <li>{@code values()} immutability and content.
 *   <li>Lowercase / kebab-case label mapping for every constant.
 *   <li>{@link EnumSet} exhaustiveness -- every constant must be reachable through the canonical
 *       switch in {@code ErrorCode}.
 *   <li>Integration: every {@link ErrorCode} resolves to a phase listed by {@link
 *       CompilerPhase#values()}, and every phase is referenced by at least one code.
 * </ul>
 *
 * <p>JUnit 6 features exercised: {@code @ParameterizedTest} with {@code @EnumSource} and
 * {@code @ValueSource}, {@code @DisplayName}, AssertJ rich assertions, lifecycle-free static
 * checks. Corner cases cover null/empty/whitespace labels, pairwise-distinct labels, immutable
 * {@code Set.of} wrapping, and {@code EnumSet} complement emptiness.
 */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.TooManyMethods",
    "checkstyle:AbbreviationAsWordInName",
    "EnumOrdinal"
})
class CompilerPhaseTest {

    // --------------------------------------------------------------------------------------------
    // enum contract
    // --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("declares exactly six compiler phases")
    void declaresExactlySixPhases() {
        assertThat(CompilerPhase.values()).hasSize(6);
    }

    @Test
    @DisplayName("exposes the canonical six phases by identity")
    void exposesCanonicalPhases() {
        assertThat(Set.of(CompilerPhase.values()))
                .containsExactlyInAnyOrder(
                        CompilerPhase.LEXER,
                        CompilerPhase.PARSER,
                        CompilerPhase.SEMANTIC,
                        CompilerPhase.IR_GENERATION,
                        CompilerPhase.CODE_GENERATION,
                        CompilerPhase.SYSTEM);
    }

    @Test
    @DisplayName("name() returns the upper-snake declaration token")
    void nameMatchesDeclaration() {
        assertThat(CompilerPhase.LEXER.name()).isEqualTo("LEXER");
        assertThat(CompilerPhase.PARSER.name()).isEqualTo("PARSER");
        assertThat(CompilerPhase.SEMANTIC.name()).isEqualTo("SEMANTIC");
        assertThat(CompilerPhase.IR_GENERATION.name()).isEqualTo("IR_GENERATION");
        assertThat(CompilerPhase.CODE_GENERATION.name()).isEqualTo("CODE_GENERATION");
        assertThat(CompilerPhase.SYSTEM.name()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("ordinals are stable and contiguous starting at zero")
    void ordinalsAreStableAndContiguous() {
        assertThat(CompilerPhase.LEXER.ordinal()).isEqualTo(0);
        assertThat(CompilerPhase.PARSER.ordinal()).isEqualTo(1);
        assertThat(CompilerPhase.SEMANTIC.ordinal()).isEqualTo(2);
        assertThat(CompilerPhase.IR_GENERATION.ordinal()).isEqualTo(3);
        assertThat(CompilerPhase.CODE_GENERATION.ordinal()).isEqualTo(4);
        assertThat(CompilerPhase.SYSTEM.ordinal()).isEqualTo(5);
    }

    @Test
    @DisplayName("valueOf round-trips every declared name")
    void valueOfRoundTripsEveryName() {
        for (final CompilerPhase phase : CompilerPhase.values()) {
            assertThat(CompilerPhase.valueOf(phase.name())).isSameAs(phase);
        }
    }

    @Test
    @DisplayName("values() returns a fresh array each call -- not a singleton")
    void valuesReturnsFreshArrayEachCall() {
        final CompilerPhase[] first = CompilerPhase.values();
        final CompilerPhase[] second = CompilerPhase.values();

        assertThat(first).isNotSameAs(second);
        assertThat(first).containsExactly(second);
    }

    @Test
    @DisplayName("values() result may be safely copied without affecting the enum")
    void valuesArrayCanBeSafelyCopied() {
        final CompilerPhase[] snapshot = CompilerPhase.values();
        final CompilerPhase[] copy = Arrays.copyOf(snapshot, snapshot.length);

        assertThat(copy).containsExactly(snapshot);
    }

    @Test
    @DisplayName("EnumSet.range covers the whole ordinal range")
    void enumSetRangeCoversEveryPhase() {
        final CompilerPhase[] phases = CompilerPhase.values();
        final Set<CompilerPhase> range = EnumSet.range(phases[0], phases[phases.length - 1]);

        assertThat(range).hasSize(phases.length);
        assertThat(range).containsExactlyInAnyOrder(phases);
    }

    @Test
    @DisplayName("all phases are distinct")
    void allPhasesAreDistinct() {
        assertThat(EnumSet.copyOf(Arrays.asList(CompilerPhase.values())))
                .hasSize(CompilerPhase.values().length);
    }

    // --------------------------------------------------------------------------------------------
    // label mapping (toString())
    // --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("LEXER labels as 'lexer'")
    void lexerLabelIsLexer() {
        assertThat(CompilerPhase.LEXER).hasToString("lexer");
    }

    @Test
    @DisplayName("PARSER labels as 'parser'")
    void parserLabelIsParser() {
        assertThat(CompilerPhase.PARSER).hasToString("parser");
    }

    @Test
    @DisplayName("SEMANTIC labels as 'semantic'")
    void semanticLabelIsSemantic() {
        assertThat(CompilerPhase.SEMANTIC).hasToString("semantic");
    }

    @Test
    @DisplayName("IR_GENERATION labels as 'ir-gen'")
    void irGenerationLabelIsIrGen() {
        assertThat(CompilerPhase.IR_GENERATION).hasToString("ir-gen");
    }

    @Test
    @DisplayName("CODE_GENERATION labels as 'codegen'")
    void codeGenerationLabelIsCodegen() {
        assertThat(CompilerPhase.CODE_GENERATION).hasToString("codegen");
    }

    @Test
    @DisplayName("SYSTEM labels as 'system'")
    void systemLabelIsSystem() {
        assertThat(CompilerPhase.SYSTEM).hasToString("system");
    }

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("every phase's label is non-blank and not equal to its name()")
    void everyPhaseLabelIsNonBlankAndDistinctFromName(final CompilerPhase phase) {
        assertThat(phase.toString()).isNotBlank();
        assertThat(phase.toString()).isNotEqualTo(phase.name());
    }

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("every phase's label contains no whitespace")
    void everyPhaseLabelHasNoWhitespace(final CompilerPhase phase) {
        assertThat(phase.toString()).doesNotContainAnyWhitespaces();
    }

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("every phase's label is lower-case")
    void everyPhaseLabelIsLowercase(final CompilerPhase phase) {
        assertThat(phase.toString()).isEqualTo(phase.toString().toLowerCase(Locale.ROOT));
    }

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("every phase's label does not start with the declaration name")
    void everyPhaseLabelDoesNotStartWithName(final CompilerPhase phase) {
        assertThat(phase.toString()).doesNotStartWith(phase.name());
    }

    @Test
    @DisplayName("phase labels are pairwise distinct")
    void phaseLabelsArePairwiseDistinct() {
        final Set<String> labels = new HashSet<>();
        for (final CompilerPhase phase : CompilerPhase.values()) {
            labels.add(phase.toString());
        }
        assertThat(labels).hasSize(CompilerPhase.values().length);
    }

    // --------------------------------------------------------------------------------------------
    // valueOf error handling
    // --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("valueOf rejects null with NullPointerException")
    void valueOfRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> CompilerPhase.valueOf(null));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                " ",
                "lexer",
                "Lexer",
                "LEXERS",
                "parser",
                "ir_generation",
                "ir-generation",
                "codegen",
                "code-generation",
                "system",
                "SYSTEM ",
                " SYSTEM",
                "lex er",
                "irgen",
            })
    @DisplayName("valueOf rejects unknown or wrong-case names with IllegalArgumentException")
    void valueOfRejectsUnknownNames(final String invalid) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> CompilerPhase.valueOf(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --------------------------------------------------------------------------------------------
    // EnumSource round-trip
    // --------------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("name() round-trips through valueOf")
    void nameRoundTripsThroughValueOf(final CompilerPhase phase) {
        assertThat(CompilerPhase.valueOf(phase.name())).isEqualTo(phase);
    }

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("every phase's label is reachable and non-empty")
    void everyPhaseLabelIsReachableAndNonEmpty(final CompilerPhase phase) {
        final String label = phase.toString();
        assertThat(label).isNotNull();
        assertThat(label).isNotEmpty();
    }

    // --------------------------------------------------------------------------------------------
    // ErrorCode integration
    // --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("every ErrorCode.phase() resolves to a CompilerPhase value")
    void everyErrorCodePhaseIsACompilerPhaseValue() {
        for (final ErrorCode code : ErrorCode.values()) {
            assertThat(code.phase())
                    .as("phase() of %s must be a real CompilerPhase constant", code)
                    .isIn((Object[]) CompilerPhase.values());
        }
    }

    @Test
    @DisplayName("every CompilerPhase value is referenced by at least one ErrorCode")
    void everyCompilerPhaseIsReferencedByAtLeastOneErrorCode() {
        final Set<CompilerPhase> referenced = EnumSet.noneOf(CompilerPhase.class);
        for (final ErrorCode code : ErrorCode.values()) {
            referenced.add(code.phase());
        }
        assertThat(referenced).containsExactlyInAnyOrder(CompilerPhase.values());
    }

    @Test
    @DisplayName("lexer error codes live in CompilerPhase.LEXER")
    void lexerErrorCodesMapToLexerPhase() {
        assertThat(ErrorCode.E0001.phase()).isEqualTo(CompilerPhase.LEXER);
        assertThat(ErrorCode.E0010.phase()).isEqualTo(CompilerPhase.LEXER);
    }

    @Test
    @DisplayName("parser error codes live in CompilerPhase.PARSER")
    void parserErrorCodesMapToParserPhase() {
        assertThat(ErrorCode.E1001.phase()).isEqualTo(CompilerPhase.PARSER);
        assertThat(ErrorCode.E1015.phase()).isEqualTo(CompilerPhase.PARSER);
    }

    @Test
    @DisplayName("semantic error codes live in CompilerPhase.SEMANTIC")
    void semanticErrorCodesMapToSemanticPhase() {
        assertThat(ErrorCode.E2001.phase()).isEqualTo(CompilerPhase.SEMANTIC);
        assertThat(ErrorCode.E2032.phase()).isEqualTo(CompilerPhase.SEMANTIC);
    }

    @Test
    @DisplayName("IR generation error codes live in CompilerPhase.IR_GENERATION")
    void irGenerationErrorCodesMapToIrPhase() {
        assertThat(ErrorCode.E3001.phase()).isEqualTo(CompilerPhase.IR_GENERATION);
        assertThat(ErrorCode.E3008.phase()).isEqualTo(CompilerPhase.IR_GENERATION);
    }

    @Test
    @DisplayName("code generation error codes live in CompilerPhase.CODE_GENERATION")
    void codeGenerationErrorCodesMapToCodeGenPhase() {
        assertThat(ErrorCode.E4001.phase()).isEqualTo(CompilerPhase.CODE_GENERATION);
        assertThat(ErrorCode.E4005.phase()).isEqualTo(CompilerPhase.CODE_GENERATION);
    }

    @Test
    @DisplayName("system error codes live in CompilerPhase.SYSTEM")
    void systemErrorCodesMapToSystemPhase() {
        assertThat(ErrorCode.E5001.phase()).isEqualTo(CompilerPhase.SYSTEM);
        assertThat(ErrorCode.E5005.phase()).isEqualTo(CompilerPhase.SYSTEM);
    }

    // --------------------------------------------------------------------------------------------
    // corner cases
    // --------------------------------------------------------------------------------------------

    @Test
    @DisplayName("phase set wrapped via Set.of is unmodifiable")
    void phaseSetWrappedViaSetOfIsUnmodifiable() {
        final Set<CompilerPhase> phases = Set.of(CompilerPhase.values());
        assertThat(phases).isUnmodifiable();
    }

    @ParameterizedTest
    @EnumSource(CompilerPhase.class)
    @DisplayName("single-element EnumSet contains exactly one phase")
    void singleElementEnumSetContainsExactlyOnePhase(final CompilerPhase phase) {
        final Set<CompilerPhase> single = EnumSet.of(phase);
        assertThat(single).containsExactly(phase);
    }

    @Test
    @DisplayName("complement of all-phases EnumSet is empty")
    void complementOfAllPhasesEnumSetIsEmpty() {
        final EnumSet<CompilerPhase> all = EnumSet.allOf(CompilerPhase.class);
        assertThat(EnumSet.complementOf(all)).isEmpty();
    }

    @Test
    @DisplayName("phase labels are never null")
    void phaseLabelsAreNeverNull() {
        for (final CompilerPhase phase : CompilerPhase.values()) {
            assertThat(phase.toString()).isNotNull();
        }
    }

    @Test
    @DisplayName("phase labels are never empty")
    void phaseLabelsAreNeverEmpty() {
        for (final CompilerPhase phase : CompilerPhase.values()) {
            assertThat(phase.toString()).isNotEmpty();
        }
    }
}
