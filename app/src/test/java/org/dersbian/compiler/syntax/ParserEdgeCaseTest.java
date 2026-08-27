package org.dersbian.compiler.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.LongVariable"
})
class ParserEdgeCaseTest {

    @Test
    void parsesEveryPrimitiveTypeAnnotation() {
        for (final String type :
                List.of(
                        "i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64", "char",
                        "string", "bool")) {
            final ParseResult result = parse("var value: " + type + " = 1");

            assertTrue(result.errors().isEmpty(), () -> type + ": " + result.errors());
            assertInstanceOf(Stmt.VarDeclaration.class, result.statements().get(0));
        }
    }

    @Test
    void parsesCustomArrayAndVectorTypes() {
        final Stmt.VarDeclaration array = declaration("var values: i32[2][3] = 1");
        assertInstanceOf(Type.Array.class, array.typeAnnotation());
        assertInstanceOf(Type.Array.class, ((Type.Array) array.typeAnnotation()).elementType());

        final Stmt.VarDeclaration custom = declaration("var item: UserType = value");
        assertEquals(new Type.Custom("UserType"), custom.typeAnnotation());

        final Stmt.VarDeclaration vector = declaration("var items: vector<i32> = value");
        assertEquals(new Type.Vector(new Type.I32()), vector.typeAnnotation());
    }

    @Test
    void parsesForWithDeclarationExpressionAndNonBlockBodies() {
        final Stmt.For declarationInitializer =
                assertInstanceOf(
                        Stmt.For.class,
                        parse("for (var i: i32 = 0; i; i++) break").statements().get(0));
        assertTrue(declarationInitializer.initializer().isPresent());

        final Stmt.For expressionInitializer =
                assertInstanceOf(
                        Stmt.For.class, parse("for (i = 0; i; i++) continue").statements().get(0));
        assertInstanceOf(Stmt.Expression.class, expressionInitializer.initializer().orElseThrow());
        assertEquals(1, expressionInitializer.body().statements().size());
    }

    @Test
    void reportsMalformedConditionsTypesAndDelimiters() {
        final ParseResult condition = parse("if condition {}");
        final ParseResult type = parse("var value: 123 = 1");
        final ParseResult vector = parse("var values: vector<> = value");

        assertTrue(condition.hasErrors());
        assertTrue(type.errors().stream().anyMatch(error -> hasCode(error, ErrorCode.E1002)));
        assertTrue(vector.hasErrors());
    }

    @Test
    void recoversFromDeeplyNestedExpressions() {
        final String source = "(".repeat(1002) + "1" + ")".repeat(1002);
        final ParseResult result = parse(source);

        assertTrue(result.errors().stream().anyMatch(error -> hasCode(error, ErrorCode.E1001)));
    }

    private Stmt.VarDeclaration declaration(final String source) {
        final ParseResult result = parse(source);
        assertTrue(result.errors().isEmpty(), () -> "Unexpected parser errors: " + result.errors());
        return assertInstanceOf(Stmt.VarDeclaration.class, result.statements().get(0));
    }

    private ParseResult parse(final String source) {
        final LexerResult lexed = new Lexer(Path.of("parser-edge.dr"), source).tokenize();
        assertTrue(lexed.errors().isEmpty(), () -> "Unexpected lexer errors: " + lexed.errors());
        return new Parser(lexed.tokens()).parse();
    }

    private boolean hasCode(final CompileError error, final ErrorCode code) {
        return error instanceof CompileError.SyntaxError syntaxError
                && syntaxError.code().orElse(null) == code;
    }
}
