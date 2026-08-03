package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.Optional;
import java.util.stream.Stream;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.dersbian.compiler.lexer.token.parser.numeric.SuffixParser.SplitResult;
import org.dersbian.compiler.lexer.token.parser.numeric.SuffixParser.SuffixPattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive test suite for {@link SuffixParser}.
 *
 * <p>Covers all public API methods including suffix detection, splitting, and routing. Includes
 * edge cases (empty strings, single characters, boundary values) and corner cases (case
 * insensitivity, ambiguous suffixes, suffix-only strings, unknown suffixes).
 *
 * <p>All assertion methods are invoked via {@link Assertions} to comply with the PMD {@code
 * TooManyStaticImports} rule while retaining full JUnit 5 semantics.
 *
 * <p>Method names follow the Checkstyle {@code AbbreviationAsWordInName} rule: single-letter type
 * abbreviations ({@code u}, {@code f}, {@code d}) are written in lowercase when embedded in a
 * camelCase identifier, e.g. {@code uSuffix}, {@code fSuffix}, {@code dSuffix}.
 */
@DisplayName("SuffixParser")
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.AvoidDuplicateLiterals",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.LongVariable",
    "PMD.GodClass",
    "PMD.CyclomaticComplexity",
    "PMD.AtLeastOneConstructor",
})
class SuffixParserTest {

    @Test
    @DisplayName("SuffixPattern.SINGLE_CHAR has character length of 1")
    void suffixPatternSingleCharHasLengthOne() {
        Assertions.assertEquals(1, SuffixPattern.SINGLE_CHAR.length());
    }

    @Test
    @DisplayName("SuffixPattern.TWO_CHAR has character length of 2")
    void suffixPatternTwoCharHasLengthTwo() {
        Assertions.assertEquals(2, SuffixPattern.TWO_CHAR.length());
    }

    @Test
    @DisplayName("SuffixPattern.THREE_CHAR has character length of 3")
    void suffixPatternThreeCharHasLengthThree() {
        Assertions.assertEquals(3, SuffixPattern.THREE_CHAR.length());
    }

    @Test
    @DisplayName("SuffixPattern enum contains exactly three constants")
    void suffixPatternEnumContainsExactlyThreeConstants() {
        Assertions.assertEquals(3, SuffixPattern.values().length);
    }

    @ParameterizedTest(name = "checkSingleCharSuffix(\"{0}\") detects SINGLE_CHAR suffix")
    @DisplayName("checkSingleCharSuffix returns SINGLE_CHAR for valid single-character suffixes")
    @ValueSource(
            strings = {
                "42u", "42U", "42f", "42F", "42d", "42D",
                "u", "f", "d", "U", "F", "D",
                "100u", "3.14f", "3.14d", "0u", "0f", "0d"
            })
    void checkSingleCharSuffixReturnsPresentForValidSuffixes(final String input) {
        final Optional<SuffixPattern> result = SuffixParser.checkSingleCharSuffix(input);

        Assertions.assertAll(
                () ->
                        Assertions.assertTrue(
                                result.isPresent(),
                                "Expected SINGLE_CHAR suffix to be detected in \"" + input + "\""),
                () -> Assertions.assertEquals(SuffixPattern.SINGLE_CHAR, result.orElse(null)));
    }

    @ParameterizedTest(name = "checkSingleCharSuffix(\"{0}\") returns empty")
    @DisplayName("checkSingleCharSuffix returns empty for strings not ending in u, f, or d")
    @ValueSource(
            strings = {
                "42", "42i", "42x", "42z", "100", "3.14", "abc", "42e", "42g", "a", "1", "0", "9"
            })
    void checkSingleCharSuffixReturnsEmptyForInvalidSuffixes(final String input) {
        final Optional<SuffixPattern> result = SuffixParser.checkSingleCharSuffix(input);

        Assertions.assertFalse(
                result.isPresent(), "Expected no SINGLE_CHAR suffix in \"" + input + "\"");
    }

    @Test
    @DisplayName("checkSingleCharSuffix returns empty for an empty string")
    void checkSingleCharSuffixReturnsEmptyForEmptyString() {
        final Optional<SuffixPattern> result = SuffixParser.checkSingleCharSuffix("");

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("checkSingleCharSuffix detects suffix even when string is a single character 'u'")
    void checkSingleCharSuffixDetectsSuffixOnSingleCharacterString() {
        final Optional<SuffixPattern> result = SuffixParser.checkSingleCharSuffix("u");

        Assertions.assertTrue(result.isPresent());
    }

    @ParameterizedTest(name = "Single char suffix ''{0}'' detected at end of ''42{0}''")
    @DisplayName("All valid single-char suffixes detected individually")
    @ValueSource(chars = {'u', 'U', 'f', 'F', 'd', 'D'})
    void checkSingleCharSuffixAllValidChars(final char suffix) {
        final String input = "42" + suffix;

        Assertions.assertTrue(
                SuffixParser.checkSingleCharSuffix(input).isPresent(),
                "Expected single-char suffix '" + suffix + "' to be detected");
    }

    @ParameterizedTest(name = "checkTwoCharSuffix(\"{0}\") detects TWO_CHAR suffix")
    @DisplayName("checkTwoCharSuffix returns TWO_CHAR for valid i8/u8 suffixes")
    @ValueSource(
            strings = {
                "42i8", "42u8", "100i8", "100u8", "0i8", "0u8",
                "42I8", "42U8", "i8", "u8", "I8", "U8"
            })
    void checkTwoCharSuffixReturnsPresentForValidSuffixes(final String input) {
        final Optional<SuffixPattern> result = SuffixParser.checkTwoCharSuffix(input);

        Assertions.assertAll(
                () ->
                        Assertions.assertTrue(
                                result.isPresent(),
                                "Expected TWO_CHAR suffix to be detected in \"" + input + "\""),
                () -> Assertions.assertEquals(SuffixPattern.TWO_CHAR, result.orElse(null)));
    }

    @ParameterizedTest(name = "checkTwoCharSuffix(\"{0}\") returns empty")
    @DisplayName("checkTwoCharSuffix returns empty for strings without valid i8/u8 suffix")
    @ValueSource(
            strings = {
                "42", "42u", "42i", "42f8", "42i9", "42u7", "42i1", "42u2", "a", "1", "", "42i16",
                "42u32"
            })
    void checkTwoCharSuffixReturnsEmptyForInvalidSuffixes(final String input) {
        final Optional<SuffixPattern> result = SuffixParser.checkTwoCharSuffix(input);

        Assertions.assertFalse(
                result.isPresent(), "Expected no TWO_CHAR suffix in \"" + input + "\"");
    }

    @Test
    @DisplayName("checkTwoCharSuffix returns empty for single-character string")
    void checkTwoCharSuffixReturnsEmptyForSingleCharString() {
        final Optional<SuffixPattern> result = SuffixParser.checkTwoCharSuffix("u");

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("checkTwoCharSuffix returns empty for empty string")
    void checkTwoCharSuffixReturnsEmptyForEmptyString() {
        final Optional<SuffixPattern> result = SuffixParser.checkTwoCharSuffix("");

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("checkTwoCharSuffix detects suffix when string is exactly 'i8'")
    void checkTwoCharSuffixDetectsExactlyI8() {
        final Optional<SuffixPattern> result = SuffixParser.checkTwoCharSuffix("i8");

        Assertions.assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("checkTwoCharSuffix detects uppercase I in '42I8'")
    void checkTwoCharSuffixUppercaseI8() {
        Assertions.assertTrue(SuffixParser.checkTwoCharSuffix("42I8").isPresent());
    }

    @Test
    @DisplayName("checkTwoCharSuffix detects uppercase U in '42U8'")
    void checkTwoCharSuffixUppercaseU8() {
        Assertions.assertTrue(SuffixParser.checkTwoCharSuffix("42U8").isPresent());
    }

    @ParameterizedTest(name = "checkThreeCharSuffix(\"{0}\") detects THREE_CHAR suffix")
    @DisplayName("checkThreeCharSuffix returns THREE_CHAR for valid i16/i32/u16/u32 suffixes")
    @ValueSource(
            strings = {
                "42i16", "42i32", "42u16", "42u32",
                "0i16", "0i32", "0u16", "0u32",
                "42I16", "42I32", "42U16", "42U32",
                "i16", "i32", "u16", "u32",
                "I16", "I32", "U16", "U32",
                "100i16", "100000u32"
            })
    void checkThreeCharSuffixReturnsPresentForValidSuffixes(final String input) {
        final Optional<SuffixPattern> result = SuffixParser.checkThreeCharSuffix(input);

        Assertions.assertAll(
                () ->
                        Assertions.assertTrue(
                                result.isPresent(),
                                "Expected THREE_CHAR suffix to be detected in \"" + input + "\""),
                () -> Assertions.assertEquals(SuffixPattern.THREE_CHAR, result.orElse(null)));
    }

    @ParameterizedTest(name = "checkThreeCharSuffix(\"{0}\") returns empty")
    @DisplayName("checkThreeCharSuffix returns empty for strings without valid three-char suffix")
    @ValueSource(
            strings = {
                "42", "42u", "42i8", "42u8", "42i17", "42i31",
                "42u15", "42u33", "42f16", "42d32", "ab", "a",
                "", "42i64", "42u64"
            })
    void checkThreeCharSuffixReturnsEmptyForInvalidSuffixes(final String input) {
        final Optional<SuffixPattern> result = SuffixParser.checkThreeCharSuffix(input);

        Assertions.assertFalse(
                result.isPresent(), "Expected no THREE_CHAR suffix in \"" + input + "\"");
    }

    @Test
    @DisplayName("checkThreeCharSuffix returns empty for two-character string")
    void checkThreeCharSuffixReturnsEmptyForTwoCharString() {
        final Optional<SuffixPattern> result = SuffixParser.checkThreeCharSuffix("i8");

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("checkThreeCharSuffix returns empty for empty string")
    void checkThreeCharSuffixReturnsEmptyForEmptyString() {
        final Optional<SuffixPattern> result = SuffixParser.checkThreeCharSuffix("");

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("checkThreeCharSuffix detects suffix when string is exactly 'u32'")
    void checkThreeCharSuffixDetectsExactlyU32() {
        final Optional<SuffixPattern> result = SuffixParser.checkThreeCharSuffix("u32");

        Assertions.assertTrue(result.isPresent());
    }

    @ParameterizedTest(name = "checkThreeCharSuffix(\"{0}\") rejects invalid digit combinations")
    @DisplayName("checkThreeCharSuffix rejects suffixes with wrong digit combinations")
    @ValueSource(
            strings = {
                "42i15", "42i33", "42u17", "42u31",
                "42i26", "42u22", "42i12", "42u18"
            })
    void checkThreeCharSuffixRejectsWrongDigitCombinations(final String input) {
        Assertions.assertFalse(SuffixParser.checkThreeCharSuffix(input).isPresent());
    }

    @Test
    @DisplayName("checkThreeCharSuffix detects uppercase I in '42I16'")
    void checkThreeCharSuffixUppercaseI16() {
        Assertions.assertTrue(SuffixParser.checkThreeCharSuffix("42I16").isPresent());
    }

    @Test
    @DisplayName("checkThreeCharSuffix detects uppercase U in '42U32'")
    void checkThreeCharSuffixUppercaseU32() {
        Assertions.assertTrue(SuffixParser.checkThreeCharSuffix("42U32").isPresent());
    }

    @Test
    @DisplayName(
            "splitNumericAndSuffix returns entire string as numeric part when no suffix present")
    void splitNumericAndSuffixNoSuffix() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits single-char suffix 'u'")
    void splitNumericAndSuffixSingleCharLowercaseU() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42u");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("u", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits single-char suffix 'f'")
    void splitNumericAndSuffixSingleCharLowercaseF() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("3.14f");

        Assertions.assertAll(
                () -> Assertions.assertEquals("3.14", result.numericPart()),
                () -> Assertions.assertEquals("f", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits single-char suffix 'd'")
    void splitNumericAndSuffixSingleCharLowercaseD() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("3.14d");

        Assertions.assertAll(
                () -> Assertions.assertEquals("3.14", result.numericPart()),
                () -> Assertions.assertEquals("d", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits two-char suffix 'i8'")
    void splitNumericAndSuffixTwoCharI8() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42i8");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("i8", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits two-char suffix 'u8'")
    void splitNumericAndSuffixTwoCharU8() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("255u8");

        Assertions.assertAll(
                () -> Assertions.assertEquals("255", result.numericPart()),
                () -> Assertions.assertEquals("u8", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits three-char suffix 'i16'")
    void splitNumericAndSuffixThreeCharI16() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("1000i16");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1000", result.numericPart()),
                () -> Assertions.assertEquals("i16", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly splits three-char suffix 'u32'")
    void splitNumericAndSuffixThreeCharU32() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("4294967295u32");

        Assertions.assertAll(
                () -> Assertions.assertEquals("4294967295", result.numericPart()),
                () -> Assertions.assertEquals("u32", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix returns empty numeric part and null suffix for empty input")
    void splitNumericAndSuffixEmptyInput() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("");

        Assertions.assertAll(
                () -> Assertions.assertEquals("", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix preserves original casing of suffix")
    void splitNumericAndSuffixPreservesCasing() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42U");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("U", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix preserves uppercase casing for three-char suffix")
    void splitNumericAndSuffixPreservesUppercaseThreeChar() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42I32");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("I32", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix with suffix-only input 'u' yields empty numeric part")
    void splitNumericAndSuffixSuffixOnlyLowercaseU() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("u");

        Assertions.assertAll(
                () -> Assertions.assertEquals("", result.numericPart()),
                () -> Assertions.assertEquals("u", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix with suffix-only input 'i8' yields empty numeric part")
    void splitNumericAndSuffixSuffixOnlyI8() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("i8");

        Assertions.assertAll(
                () -> Assertions.assertEquals("", result.numericPart()),
                () -> Assertions.assertEquals("i8", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix with suffix-only input 'u16' yields empty numeric part")
    void splitNumericAndSuffixSuffixOnlyU16() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("u16");

        Assertions.assertAll(
                () -> Assertions.assertEquals("", result.numericPart()),
                () -> Assertions.assertEquals("u16", result.suffix()));
    }

    @Test
    @DisplayName(
            "splitNumericAndSuffix: single-char suffix takes priority when last char is 'f' "
                    + "even if 'i32' could match (e.g. string ending in ...f does not match i32)")
    void splitNumericAndSuffixSingleCharPriorityOverThreeChar() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42f");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("f", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix: string '42d' matches single-char suffix 'd', not 'i32'")
    void splitNumericAndSuffixSingleCharLowercasedTakesPriority() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("42d");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("d", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix: string ending with digit not matching any suffix")
    void splitNumericAndSuffixNoSuffixDigitEnding() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("12345");

        Assertions.assertAll(
                () -> Assertions.assertEquals("12345", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix: floating point without suffix returns no suffix")
    void splitNumericAndSuffixFloatingPointNoSuffix() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("3.14");

        Assertions.assertAll(
                () -> Assertions.assertEquals("3.14", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName(
            "splitNumericAndSuffix: '1u32' detects three-char suffix 'u32' since last char "
                    + "'2' does not match single-char")
    void splitNumericAndSuffixThreeCharDetectedWhenSingleCharDoesNotMatch() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("1u32");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1", result.numericPart()),
                () -> Assertions.assertEquals("u32", result.suffix()));
    }

    @Test
    @DisplayName(
            "splitNumericAndSuffix: '1u8' detects two-char suffix 'u8' since last char '8' "
                    + "does not match single-char and three-char fails")
    void splitNumericAndSuffixTwoCharDetectedCorrectly() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("1u8");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1", result.numericPart()),
                () -> Assertions.assertEquals("u8", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly handles '1u16' without false single-char match")
    void splitNumericAndSuffixAmbiguousU16() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("1u16");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1", result.numericPart()),
                () -> Assertions.assertEquals("u16", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix correctly handles '1i32' without false single-char match")
    void splitNumericAndSuffixAmbiguousI32() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("1i32");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1", result.numericPart()),
                () -> Assertions.assertEquals("i32", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix does not false-detect suffix in '1868'")
    void splitNumericAndSuffixNoFalseDetectionOnPureNumber() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("1868");

        Assertions.assertAll(
                () -> Assertions.assertEquals("1868", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix does not false-detect suffix in '316'")
    void splitNumericAndSuffixNoFalseDetectionOnThreeDigitNumber() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("316");

        Assertions.assertAll(
                () -> Assertions.assertEquals("316", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix with non-numeric non-suffix string returns no suffix")
    void splitNumericAndSuffixNonNumericString() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("abc");

        Assertions.assertAll(
                () -> Assertions.assertEquals("abc", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix handles very long numeric string with suffix")
    void splitNumericAndSuffixVeryLongNumericWithSuffix() {
        final String longNumber = "9".repeat(50) + "u";
        final SplitResult result = SuffixParser.splitNumericAndSuffix(longNumber);

        Assertions.assertAll(
                () -> Assertions.assertEquals("9".repeat(50), result.numericPart()),
                () -> Assertions.assertEquals("u", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix handles very long numeric string without suffix")
    void splitNumericAndSuffixVeryLongNumericWithoutSuffix() {
        final String longNumber = "9".repeat(50);
        final SplitResult result = SuffixParser.splitNumericAndSuffix(longNumber);

        Assertions.assertAll(
                () -> Assertions.assertEquals(longNumber, result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix handles negative number with suffix")
    void splitNumericAndSuffixNegativeWithSuffix() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("-42f");

        Assertions.assertAll(
                () -> Assertions.assertEquals("-42", result.numericPart()),
                () -> Assertions.assertEquals("f", result.suffix()));
    }

    @Test
    @DisplayName("splitNumericAndSuffix handles negative number without suffix")
    void splitNumericAndSuffixNegativeWithoutSuffix() {
        final SplitResult result = SuffixParser.splitNumericAndSuffix("-42");

        Assertions.assertAll(
                () -> Assertions.assertEquals("-42", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("handleSuffix with null suffix and integer input delegates to default handler")
    void handleSuffixNullSuffixIntegerInput() {
        final INumber result = SuffixParser.handleSuffix("42", null);

        Assertions.assertNotNull(result, "Expected non-null result for integer without suffix");
        Assertions.assertInstanceOf(INumber.Integer.class, result);
        Assertions.assertEquals(42L, ((INumber.Integer) result).value());
    }

    @Test
    @DisplayName(
            "handleSuffix with null suffix and floating-point input delegates to default handler")
    void handleSuffixNullSuffixFloatInput() {
        final INumber result = SuffixParser.handleSuffix("3.14", null);

        Assertions.assertNotNull(result, "Expected non-null result for float without suffix");
        Assertions.assertInstanceOf(INumber.Float64.class, result);
    }

    @Test
    @DisplayName("handleSuffix with 'u' suffix parses unsigned 64-bit integer")
    void handleSuffixLowercaseuSuffixParsesUnsigned64() {
        final INumber result = SuffixParser.handleSuffix("42", "u");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);
        Assertions.assertEquals(42L, ((INumber.UnsignedInteger) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'U' suffix (uppercase) parses unsigned 64-bit integer")
    void handleSuffixUppercaseuSuffixParsesUnsigned64() {
        final INumber result = SuffixParser.handleSuffix("42", "U");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);
    }

    @Test
    @DisplayName("handleSuffix with 'u8' suffix parses value within u8 range")
    void handleSuffixU8ParsesValueInRange() {
        final INumber result = SuffixParser.handleSuffix("255", "u8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U8.class, result);
        Assertions.assertEquals((short) 255, ((INumber.U8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u8' suffix returns null for value exceeding u8 range")
    void handleSuffixU8ReturnsNullWhenOverflow() {
        final INumber result = SuffixParser.handleSuffix("256", "u8");

        Assertions.assertNull(result, "Expected null for u8 value exceeding 255");
    }

    @Test
    @DisplayName("handleSuffix with 'u8' suffix parses zero")
    void handleSuffixU8ParsesZero() {
        final INumber result = SuffixParser.handleSuffix("0", "u8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U8.class, result);
        Assertions.assertEquals((short) 0, ((INumber.U8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u8' suffix and value 0 returns U8(0)")
    void handleSuffixU8BoundaryMin() {
        final INumber result = SuffixParser.handleSuffix("0", "u8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U8.class, result);
        Assertions.assertEquals((short) 0, ((INumber.U8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u8' suffix and max value 255 returns U8(255)")
    void handleSuffixU8BoundaryMax() {
        final INumber result = SuffixParser.handleSuffix("255", "u8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U8.class, result);
        Assertions.assertEquals((short) 255, ((INumber.U8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u16' suffix parses value within u16 range")
    void handleSuffixU16ParsesValueInRange() {
        final INumber result = SuffixParser.handleSuffix("65535", "u16");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U16.class, result);
        Assertions.assertEquals(65_535, ((INumber.U16) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u16' suffix returns null for value exceeding u16 range")
    void handleSuffixU16ReturnsNullWhenOverflow() {
        final INumber result = SuffixParser.handleSuffix("65536", "u16");

        Assertions.assertNull(result, "Expected null for u16 value exceeding 65535");
    }

    @Test
    @DisplayName("handleSuffix with 'u16' suffix and max value 65535 returns U16(65535)")
    void handleSuffixU16BoundaryMax() {
        final INumber result = SuffixParser.handleSuffix("65535", "u16");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U16.class, result);
        Assertions.assertEquals(65_535, ((INumber.U16) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u32' suffix parses value within u32 range")
    void handleSuffixU32ParsesValueInRange() {
        final INumber result = SuffixParser.handleSuffix("4294967295", "u32");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U32.class, result);
        Assertions.assertEquals(4_294_967_295L, ((INumber.U32) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u32' suffix returns null for value exceeding u32 range")
    void handleSuffixU32ReturnsNullWhenOverflow() {
        final INumber result = SuffixParser.handleSuffix("4294967296", "u32");

        Assertions.assertNull(result, "Expected null for u32 value exceeding 4294967295");
    }

    @Test
    @DisplayName("handleSuffix with 'u32' suffix parses zero")
    void handleSuffixU32ParsesZero() {
        final INumber result = SuffixParser.handleSuffix("0", "u32");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U32.class, result);
        Assertions.assertEquals(0L, ((INumber.U32) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'u32' suffix and max value 4294967295 returns U32(4294967295)")
    void handleSuffixU32BoundaryMax() {
        final INumber result = SuffixParser.handleSuffix("4294967295", "u32");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.U32.class, result);
        Assertions.assertEquals(4_294_967_295L, ((INumber.U32) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i8' suffix parses value within i8 range")
    void handleSuffixI8ParsesValueInRange() {
        final INumber result = SuffixParser.handleSuffix("127", "i8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I8.class, result);
        Assertions.assertEquals((byte) 127, ((INumber.I8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i8' suffix returns null for value exceeding i8 range")
    void handleSuffixI8ReturnsNullWhenOverflow() {
        final INumber result = SuffixParser.handleSuffix("128", "i8");

        Assertions.assertNull(result, "Expected null for i8 value exceeding 127");
    }

    @Test
    @DisplayName("handleSuffix with 'i8' suffix parses zero")
    void handleSuffixI8ParsesZero() {
        final INumber result = SuffixParser.handleSuffix("0", "i8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I8.class, result);
        Assertions.assertEquals((byte) 0, ((INumber.I8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i8' suffix and max value 127 returns I8(127)")
    void handleSuffixI8BoundaryMax() {
        final INumber result = SuffixParser.handleSuffix("127", "i8");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I8.class, result);
        Assertions.assertEquals((byte) 127, ((INumber.I8) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i16' suffix parses value within i16 range")
    void handleSuffixI16ParsesValueInRange() {
        final INumber result = SuffixParser.handleSuffix("32767", "i16");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I16.class, result);
        Assertions.assertEquals((short) 32_767, ((INumber.I16) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i16' suffix returns null for value exceeding i16 range")
    void handleSuffixI16ReturnsNullWhenOverflow() {
        final INumber result = SuffixParser.handleSuffix("32768", "i16");

        Assertions.assertNull(result, "Expected null for i16 value exceeding 32767");
    }

    @Test
    @DisplayName("handleSuffix with 'i16' suffix and max value 32767 returns I16(32767)")
    void handleSuffixI16BoundaryMax() {
        final INumber result = SuffixParser.handleSuffix("32767", "i16");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I16.class, result);
        Assertions.assertEquals((short) 32_767, ((INumber.I16) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i32' suffix parses value within i32 range")
    void handleSuffixI32ParsesValueInRange() {
        final INumber result = SuffixParser.handleSuffix("2147483647", "i32");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I32.class, result);
        Assertions.assertEquals(2_147_483_647, ((INumber.I32) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'i32' suffix returns null for value exceeding i32 range")
    void handleSuffixI32ReturnsNullWhenOverflow() {
        final INumber result = SuffixParser.handleSuffix("2147483648", "i32");

        Assertions.assertNull(result, "Expected null for i32 value exceeding 2147483647");
    }

    @Test
    @DisplayName("handleSuffix with 'i32' suffix and max value 2147483647 returns I32(2147483647)")
    void handleSuffixI32BoundaryMax() {
        final INumber result = SuffixParser.handleSuffix("2147483647", "i32");

        Assertions.assertNotNull(result);
        Assertions.assertInstanceOf(INumber.I32.class, result);
        Assertions.assertEquals(2_147_483_647, ((INumber.I32) result).value());
    }

    @Test
    @DisplayName("handleSuffix with 'f' suffix parses float32 value")
    void handleSuffixLowercasefSuffixParsesFloat32() {
        final INumber result = SuffixParser.handleSuffix("3.14", "f");

        Assertions.assertNotNull(result);
    }

    @Test
    @DisplayName("handleSuffix with 'F' suffix (uppercase) parses float32 value")
    void handleSuffixUppercasefSuffixParsesFloat32() {
        final INumber result = SuffixParser.handleSuffix("3.14", "F");

        Assertions.assertNotNull(result);
    }

    @Test
    @DisplayName("handleSuffix with 'd' suffix parses as default (float64/integer)")
    void handleSuffixLowercasedSuffixDelegatesToDefault() {
        final INumber result = SuffixParser.handleSuffix("3.14", "d");

        Assertions.assertNotNull(result);
    }

    @Test
    @DisplayName("handleSuffix with 'D' suffix (uppercase) parses as default")
    void handleSuffixUppercasedSuffixDelegatesToDefault() {
        final INumber result = SuffixParser.handleSuffix("42", "D");

        Assertions.assertNotNull(result);
    }

    @Test
    @DisplayName(
            "handleSuffix with 'd' suffix and integer numeric part delegates to default handler")
    void handleSuffixLowercasedSuffixWithIntegerDelegatesToDefault() {
        final INumber result = SuffixParser.handleSuffix("42", "d");

        Assertions.assertNotNull(
                result,
                "Expected non-null: 'd' suffix should delegate to default handler for '42'");
    }

    @ParameterizedTest(name = "handleSuffix(\"42\", \"{0}\") returns null for unknown suffix")
    @DisplayName("handleSuffix returns null for unknown or unsupported suffixes")
    @ValueSource(
            strings = {
                "x", "i64", "u64", "f64", "long", "int", "byte", "short", "ll", "uu", "ff", "dd",
                "i", "abc", "z"
            })
    void handleSuffixReturnsNullForUnknownSuffix(final String suffix) {
        final INumber result = SuffixParser.handleSuffix("42", suffix);

        Assertions.assertNull(result, "Expected null for unknown suffix \"" + suffix + "\"");
    }

    @ParameterizedTest(name = "handleSuffix(\"100\", \"{0}\") is case-insensitive")
    @DisplayName("handleSuffix is case-insensitive for all valid suffixes")
    @CsvSource({
        "u,   u",
        "U,   u",
        "u8,  u8",
        "U8,  u8",
        "u16, u16",
        "U16, u16",
        "u32, u32",
        "U32, u32",
        "i8,  i8",
        "I8,  i8",
        "i16, i16",
        "I16, i16",
        "i32, i32",
        "I32, i32",
        "f,   f",
        "F,   f",
        "d,   d",
        "D,   d"
    })
    void handleSuffixCaseInsensitive(final String suffix, final String expectedNormalized) {
        final INumber result = SuffixParser.handleSuffix("100", suffix);

        Assertions.assertNotNull(
                result, "Expected non-null result for case-insensitive suffix \"" + suffix + "\"");
    }

    @Test
    @DisplayName(
            "handleSuffix with empty numeric part and 'u' suffix returns null or handles"
                    + " gracefully")
    void handleSuffixEmptyNumericPartWithLowercaseuSuffix() {
        final INumber result = SuffixParser.handleSuffix("", "u");

        Assertions.assertNull(result, "Expected null for empty numeric part with 'u' suffix");
    }

    @Test
    @DisplayName(
            "handleSuffix with empty numeric part and null suffix returns null or handles"
                    + " gracefully")
    void handleSuffixEmptyNumericPartNullSuffix() {
        final INumber result = SuffixParser.handleSuffix("", null);

        Assertions.assertNull(result, "Expected null for empty numeric part with no suffix");
    }

    @Test
    @DisplayName("handleSuffix with non-numeric string and 'u' suffix returns null")
    void handleSuffixNonNumericWithLowercaseuSuffix() {
        final INumber result = SuffixParser.handleSuffix("abc", "u");

        Assertions.assertNull(result, "Expected null for non-numeric input with 'u' suffix");
    }

    @Test
    @DisplayName("handleSuffix with non-numeric string and 'i8' suffix returns null")
    void handleSuffixNonNumericWithI8Suffix() {
        final INumber result = SuffixParser.handleSuffix("abc", "i8");

        Assertions.assertNull(result, "Expected null for non-numeric input with 'i8' suffix");
    }

    @Test
    @DisplayName("handleSuffix with non-numeric string and null suffix returns null")
    void handleSuffixNonNumericWithNullSuffix() {
        final INumber result = SuffixParser.handleSuffix("abc", null);

        Assertions.assertNull(result, "Expected null for non-numeric input with null suffix");
    }

    private static Stream<Arguments> overflowArguments() {
        return Stream.of(
                Arguments.of("256", "u8"),
                Arguments.of("65536", "u16"),
                Arguments.of("4294967296", "u32"),
                Arguments.of("128", "i8"),
                Arguments.of("32768", "i16"),
                Arguments.of("2147483648", "i32"));
    }

    @ParameterizedTest(name = "handleSuffix(\"{0}\", \"{1}\") returns null for overflow")
    @DisplayName("handleSuffix returns null when numeric value exceeds the type range")
    @MethodSource("overflowArguments")
    void handleSuffixReturnsNullForOverflow(final String numericPart, final String suffix) {
        final INumber result = SuffixParser.handleSuffix(numericPart, suffix);

        Assertions.assertNull(
                result,
                "Expected null for overflow of suffix \""
                        + suffix
                        + "\" with value \""
                        + numericPart
                        + "\"");
    }

    private static Stream<Arguments> splitAndHandleRoundTripArguments() {
        return Stream.of(
                Arguments.of("42u", INumber.UnsignedInteger.class),
                Arguments.of("255u8", INumber.U8.class),
                Arguments.of("1000u16", INumber.U16.class),
                Arguments.of("100000u32", INumber.U32.class),
                Arguments.of("127i8", INumber.I8.class),
                Arguments.of("32767i16", INumber.I16.class),
                Arguments.of("2147483647i32", INumber.I32.class),
                Arguments.of("42", INumber.Integer.class));
    }

    @ParameterizedTest(name = "split then handle \"{0}\" yields {1}")
    @DisplayName("splitNumericAndSuffix followed by handleSuffix produces correct INumber type")
    @MethodSource("splitAndHandleRoundTripArguments")
    void splitAndHandleRoundTrip(
            final String literal, final Class<? extends INumber> expectedType) {
        final SplitResult split = SuffixParser.splitNumericAndSuffix(literal);
        final INumber result = SuffixParser.handleSuffix(split.numericPart(), split.suffix());

        Assertions.assertNotNull(
                result, "Expected non-null result for literal \"" + literal + "\"");
        Assertions.assertInstanceOf(
                expectedType,
                result,
                "Expected type "
                        + expectedType.getSimpleName()
                        + " for literal \""
                        + literal
                        + "\"");
    }

    @Test
    @DisplayName("SplitResult correctly stores numeric part and suffix")
    void splitResultStoresValues() {
        final SplitResult result = new SplitResult("42", "u");

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertEquals("u", result.suffix()));
    }

    @Test
    @DisplayName("SplitResult with null suffix stores null correctly")
    void splitResultWithNullSuffix() {
        final SplitResult result = new SplitResult("42", null);

        Assertions.assertAll(
                () -> Assertions.assertEquals("42", result.numericPart()),
                () -> Assertions.assertNull(result.suffix()));
    }

    @Test
    @DisplayName("SplitResult equals and hashCode follow record semantics")
    void splitResultEqualsAndHashCode() {
        final SplitResult result1 = new SplitResult("42", "u");
        final SplitResult result2 = new SplitResult("42", "u");
        final SplitResult result3 = new SplitResult("42", "f");

        Assertions.assertAll(
                () -> Assertions.assertEquals(result1, result2),
                () -> Assertions.assertEquals(result1.hashCode(), result2.hashCode()),
                () -> Assertions.assertNotEquals(result1, result3));
    }
}
