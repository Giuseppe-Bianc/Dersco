package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AtLeastOneConstructor")
class ParseResultTest {

    @Test
    void emptyParseResultHasNoErrorsAndNoStatements() {
        final ParseResult result = new ParseResult(List.of(), List.of());
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.errors()).isEmpty();
        assertThat(result.statements()).isEmpty();
    }

    @Test
    void parseResultWithErrorsReportsHasErrors() {
        final CompileError.SyntaxError error =
                CompileError.syntaxError(
                        null, "test", Span.point(SourceLocation.create(1, 1, 0L)), null);
        final ParseResult result = new ParseResult(List.of(), List.of(error));
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.errors()).hasSize(1);
    }

    @Test
    void statementsListIsUnmodifiable() {
        final ParseResult result = new ParseResult(List.of(), List.of());
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> result.statements().add(null));
    }

    @Test
    void errorsListIsUnmodifiable() {
        final ParseResult result = new ParseResult(List.of(), List.of());
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class, () -> result.errors().add(null));
    }

    @Test
    void nullStatementsThrowsNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ParseResult(null, List.of()))
                .withMessageContaining("statements");
    }

    @Test
    void nullErrorsThrowsNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ParseResult(List.of(), null))
                .withMessageContaining("errors");
    }
}
