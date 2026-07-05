package org.dersbian.compiler.error;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.CommentRequired", "PMD.UnitTestAssertionsShouldIncludeMessage"})
class ErrorCodeTest {

  @Test
  void coreMetadataMatchesTheRustModel() {
    Assertions.assertAll(
        () -> Assertions.assertEquals("E0001", ErrorCode.E0001.code()),
        () -> Assertions.assertEquals(1, ErrorCode.E0001.numericCode()),
        () -> Assertions.assertEquals(CompilerPhase.LEXER, ErrorCode.E0001.phase()),
        () -> Assertions.assertEquals(Severity.ERROR, ErrorCode.E0001.severity()),
        () -> Assertions.assertEquals("invalid or unrecognized token", ErrorCode.E0001.message()),
        () -> Assertions.assertEquals("E2023: undefined variable", ErrorCode.E2023.toString()));
  }

  @Test
  void warningAndSuggestionMetadataArePreserved() {
    Assertions.assertAll(
        () -> Assertions.assertEquals(Severity.WARNING, ErrorCode.E1013.severity()),
        () -> Assertions.assertEquals(CompilerPhase.PARSER, ErrorCode.E1013.phase()),
        () -> Assertions.assertTrue(ErrorCode.E1013.suggestions().isEmpty()),
        () ->
            Assertions.assertEquals(
                List.of(
                    "Declare the variable: var x: i32 = 0",
                    "Check for typos in the variable name",
                    "Ensure the variable is in scope"),
                ErrorCode.E2023.suggestions()));
  }

  @Test
  void explanationIsNonEmptyForRepresentativeCodes() {
    Assertions.assertAll(
        () -> Assertions.assertFalse(ErrorCode.E2023.explanation().isEmpty()),
        () -> Assertions.assertTrue(ErrorCode.E2023.explanation().contains("Declare")),
        () -> Assertions.assertFalse(ErrorCode.E0005.explanation().isEmpty()));
  }
}