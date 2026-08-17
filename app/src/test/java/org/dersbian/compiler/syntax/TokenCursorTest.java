package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnitTestContainsTooManyAsserts"})
class TokenCursorTest {

    private static final SourceId.Generated SRC = new SourceId.Generated("test");

    private static Token tok(final TokenKind kind) {
        return Token.create(SRC, kind, Span.point(SourceLocation.create(1, 1, 0L)));
    }

    private static List<Token> tokens(final TokenKind... kinds) {
        final List<Token> list = new ArrayList<>();
        for (final TokenKind k : kinds) {
            list.add(tok(k));
        }
        return list;
    }

    @Test
    void peekReturnsFirstNonCommentToken() {
        final TokenCursor cursor =
                new TokenCursor(
                        tokens(TokenKind.Simple.Special.COMMENT, TokenKind.Simple.Operator.PLUS));
        assertThat(cursor.peek().type()).isEqualTo(TokenKind.Simple.Operator.PLUS);
    }

    @Test
    void advanceConsumesAndReturnsToken() {
        final TokenCursor cursor =
                new TokenCursor(
                        tokens(TokenKind.Simple.Operator.PLUS, TokenKind.Simple.Operator.MINUS));
        assertThat(cursor.advance().type()).isEqualTo(TokenKind.Simple.Operator.PLUS);
        assertThat(cursor.advance().type()).isEqualTo(TokenKind.Simple.Operator.MINUS);
    }

    @Test
    void isAtEndTrueWhenOnlyEofRemains() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Special.EOF));
        assertThat(cursor.isAtEnd()).isTrue();
    }

    @Test
    void isAtEndFalseWhenTokensRemain() {
        final TokenCursor cursor =
                new TokenCursor(
                        tokens(TokenKind.Simple.Operator.PLUS, TokenKind.Simple.Special.EOF));
        assertThat(cursor.isAtEnd()).isFalse();
    }

    @Test
    void checkReturnsTrueForMatchingKind() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Operator.PLUS));
        assertThat(cursor.check(TokenKind.Simple.Operator.PLUS)).isTrue();
    }

    @Test
    void checkReturnsFalseForNonMatchingKind() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Operator.PLUS));
        assertThat(cursor.check(TokenKind.Simple.Operator.MINUS)).isFalse();
    }

    @Test
    void expectAdvancesOnMatch() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Operator.PLUS));
        final List<CompileError.SyntaxError> errs = new ArrayList<>();
        final Token got = cursor.expect(TokenKind.Simple.Operator.PLUS, errs);
        assertThat(got.type()).isEqualTo(TokenKind.Simple.Operator.PLUS);
        assertThat(errs).isEmpty();
    }

    @Test
    void expectAddsErrorOnMismatch() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Operator.MINUS));
        final List<CompileError.SyntaxError> errs = new ArrayList<>();
        cursor.expect(TokenKind.Simple.Operator.PLUS, errs);
        assertThat(errs).hasSize(1);
    }

    @Test
    void multilineCommentIsFiltered() {
        final TokenCursor cursor =
                new TokenCursor(
                        tokens(
                                TokenKind.Simple.Special.MULTILINE_COMMENT,
                                TokenKind.Simple.Operator.STAR));
        assertThat(cursor.peek().type()).isEqualTo(TokenKind.Simple.Operator.STAR);
    }

    @Test
    void synchronizeSkipsToBrace() {
        final TokenCursor cursor =
                new TokenCursor(
                        tokens(
                                TokenKind.Simple.Operator.PLUS,
                                TokenKind.Simple.Operator.MINUS,
                                TokenKind.Simple.Delimiter.CLOSE_BRACE,
                                TokenKind.Simple.Operator.STAR));
        final List<CompileError.SyntaxError> errs = new ArrayList<>();
        cursor.synchronize(errs);
        assertThat(cursor.peek().type()).isEqualTo(TokenKind.Simple.Operator.STAR);
    }

    @Test
    void synchronizeStopsAtStatementKeyword() {
        final TokenCursor cursor =
                new TokenCursor(
                        tokens(
                                TokenKind.Simple.Operator.PLUS,
                                TokenKind.Simple.Operator.MINUS,
                                TokenKind.Simple.Keyword.FUN));
        final List<CompileError.SyntaxError> errs = new ArrayList<>();
        cursor.synchronize(errs);
        assertThat(cursor.peek().type()).isEqualTo(TokenKind.Simple.Keyword.FUN);
    }

    @Test
    void currentSpanReturnsSpanOfPeekToken() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Operator.PLUS));
        assertThat(cursor.currentSpan()).isEqualTo(cursor.peek().span());
    }

    @Test
    void peekOnEmptyStreamReturnsEof() {
        final TokenCursor cursor = new TokenCursor(List.of());
        assertThat(cursor.peek().type()).isEqualTo(TokenKind.Simple.Special.EOF);
    }

    @Test
    void advancePastEofReturnsEof() {
        final TokenCursor cursor = new TokenCursor(tokens(TokenKind.Simple.Special.EOF));
        assertThat(cursor.advance().type()).isEqualTo(TokenKind.Simple.Special.EOF);
        assertThat(cursor.advance().type()).isEqualTo(TokenKind.Simple.Special.EOF);
    }
}
