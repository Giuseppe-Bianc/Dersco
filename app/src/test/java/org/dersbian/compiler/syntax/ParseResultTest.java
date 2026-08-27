package org.dersbian.compiler.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.junit.jupiter.api.Test;

class ParseResultTest {

    @Test
    void defensivelyCopiesStatementsAndErrors() {
        final List<Stmt> statements = new ArrayList<>();
        final List<CompileError> errors = new ArrayList<>();
        final ParseResult result = new ParseResult(statements, errors);

        statements.add(new Stmt.Block(List.of(), Span.point(SourceLocation.create(1, 1, 0))));
        errors.clear();

        assertEquals(0, result.statements().size());
        assertEquals(0, result.errors().size());
    }

    @Test
    void rejectsNullLists() {
        assertThrows(NullPointerException.class, () -> new ParseResult(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ParseResult(List.of(), null));
    }
}