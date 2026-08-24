package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts"
})
class ParserTest {

    private ParseResult parseSource(final String source) {
        final Lexer lexer = new Lexer(Path.of("test.dr"), source);
        final LexerResult result = lexer.tokenize();
        return new Parser(result.tokens(), Path.of("test.dr")).parse();
    }

    private ParseResult parseFile(final Path drFile) throws IOException {
        final String source = Files.readString(drFile);
        final Lexer lexer = new Lexer(drFile, source);
        final LexerResult result = lexer.tokenize();
        return new Parser(result.tokens(), drFile).parse();
    }

    @Test
    void emptyMainBlockParsesWithNoErrors() {
        final ParseResult result = parseSource("main { }\n");
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.statements()).hasSize(1);
        assertThat(result.statements().get(0)).isInstanceOf(Stmt.MainFunction.class);
        final Stmt.MainFunction main = (Stmt.MainFunction) result.statements().get(0);
        assertThat(main.body().statements()).isEmpty();
    }

    @Test
    void largeToyProgramParsesWithNoErrors() throws IOException {
        final ParseResult result = parseFile(Path.of("dr_files/large_toy_program.dr"));
        assertThat(result.hasErrors()).isFalse();
        final List<Stmt> stmts = result.statements();
        assertThat(stmts.stream().anyMatch(s -> s instanceof Stmt.MainFunction)).isTrue();
        assertThat(stmts.stream().filter(s -> s instanceof Stmt.Function).count())
                .isGreaterThanOrEqualTo(1L);
    }

    @Test
    void simpleToyParsesWithNoErrors() throws IOException {
        final ParseResult result = parseFile(Path.of("dr_files/simple_test.dr"));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void inputDrParsesWithNoErrors() throws IOException {
        final ParseResult result = parseFile(Path.of("dr_files/input.dr"));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void breakContinueLoopsParsesWithNoErrors() throws IOException {
        final ParseResult result = parseFile(Path.of("dr_files/break_continue_loops.dr"));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void testForFileParsesWithNoErrors() throws IOException {
        final ParseResult result = parseFile(Path.of("dr_files/test_for.dr"));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void parseLargeToyProgramUnder100ms() throws IOException {
        final Path drFile = Path.of("dr_files/large_toy_program.dr");
        final String source = Files.readString(drFile);
        final Lexer lexer = new Lexer(drFile, source);
        final LexerResult result = lexer.tokenize();
        final Parser parser = new Parser(result.tokens(), drFile);
        final long start = System.nanoTime();
        parser.parse();
        final long elapsed = System.nanoTime() - start;
        assertThat(elapsed).isLessThan(100_000_000L);
    }

    @Test
    void parseResultStatementsListIsUnmodifiable() {
        final ParseResult result = parseSource("main { }\n");
        assertThatThrownBy(() -> result.statements().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
