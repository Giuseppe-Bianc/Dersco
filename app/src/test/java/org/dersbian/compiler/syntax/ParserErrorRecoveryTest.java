package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Path;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts"
})
class ParserErrorRecoveryTest {

    private ParseResult parseSource(final String source) {
        final Lexer lexer = new Lexer(Path.of("test.dr"), source);
        final LexerResult result = lexer.tokenize();
        return new Parser(result.tokens(), Path.of("test.dr")).parse();
    }

    @Test
    void missingColonInVarDeclAddsError() {
        final ParseResult result = parseSource("var x i32 = 5i32\n");
        assertThat(result.hasErrors()).isTrue();
        final String msg = result.errors().get(0).errorMessage().toLowerCase(Locale.ROOT);
        assertThat(msg).contains("expected");
        assertThat(msg).contains("':'");
    }

    @Test
    void missingClosingParenInCallAddsError() {
        final ParseResult result = parseSource("main { f(1i32 }\n");
        assertThat(result.errors()).isNotEmpty();
        final boolean hasCloseParen =
                result.errors().stream()
                        .anyMatch(
                                e ->
                                        e.errorMessage().contains(")")
                                                || e.errorMessage().contains("')'"));
        assertThat(hasCloseParen).isTrue();
    }

    @Test
    void unexpectedEofMidExpressionAddsError() {
        final ParseResult result = parseSource("main { var x: i32 = 1i32 +\n");
        assertThat(result.hasErrors()).isTrue();
        final boolean mentionsEof =
                result.errors().stream()
                        .anyMatch(
                                e -> {
                                    final String m = e.errorMessage().toLowerCase(Locale.ROOT);
                                    return m.contains("eof") || m.contains("end of");
                                });
        assertThat(mentionsEof).isTrue();
    }

    @Test
    void multipleErrorsAreAllCollected() {
        final ParseResult result = parseSource("main { var : i32 var : bool }\n");
        assertThat(result.errors().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void parseDoesNotThrowOnSyntaxError() {
        assertThatCode(() -> parseSource("fun {\n")).doesNotThrowAnyException();
    }

    @Test
    void errorSpanPointsToOffendingToken() {
        final ParseResult result = parseSource("var x: i32 = +\n");
        assertThat(result.hasErrors()).isTrue();
        final long startOffset = result.errors().get(0).errorSpan().start().offset();
        assertThat(startOffset).isGreaterThan(0L);
    }

    @Test
    void recoveryAfterErrorContinuesParsingNextTopLevelDecl() {
        final ParseResult result = parseSource("fun bad { } fun good(): void { }\n");
        assertThat(result.errors()).isNotEmpty();
        final boolean hasGoodFunction =
                result.statements().stream().anyMatch(s -> s instanceof Stmt.Function);
        assertThat(hasGoodFunction).isTrue();
    }

    @Test
    void missingFunctionReturnTypeAddsError() {
        final ParseResult result = parseSource("fun f() { }\n");
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void missingFunctionNameAddsError() {
        final ParseResult result = parseSource("fun (a: i32): void { }\n");
        assertThat(result.errors()).isNotEmpty();
    }
}
