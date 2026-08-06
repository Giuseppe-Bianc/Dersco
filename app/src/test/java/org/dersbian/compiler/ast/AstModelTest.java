package org.dersbian.compiler.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.LongVariable"
})
class AstModelTest {

    private static final String COUNT = "count";
    private static final Span RANGE =
            Span.create(SourceLocation.create(1, 1, 0L), SourceLocation.create(1, 2, 1L));

    @Test
    void nodesKeepMandatoryRangesAndDefensivelyCopyChildren() {
        final List<Stmt> statements = new ArrayList<>();
        final Block block = new Block(statements, RANGE);

        statements.add(new BreakStmt(RANGE));

        assertThat(block.range()).isEqualTo(RANGE);
        assertThat(block.statements()).isEmpty();
        assertThatThrownBy(() -> block.statements().add(new ContinueStmt(RANGE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void optionalSyntacticComponentsCanBeAbsent() {
        final VariableDecl inferred =
                new VariableDecl(COUNT, null, new IntLiteral(1L, RANGE), RANGE);
        final ForStmt openEnded = new ForStmt(null, null, null, new Block(List.of(), RANGE), RANGE);

        assertThat(inferred.declaredType()).isNull();
        assertThat(openEnded.init()).isNull();
        assertThat(openEnded.condition()).isNull();
        assertThat(openEnded.update()).isNull();
    }

    @Test
    void semanticAnnotationsDoNotChangeSyntacticShape() {
        final NameExpr reference = new NameExpr(COUNT, RANGE);
        final VariableDecl declaration =
                new VariableDecl(
                        COUNT, new PrimitiveTypeNode(PrimitiveTypeName.INT, RANGE), null, RANGE);
        final Symbol symbol = new Symbol(COUNT, SymbolKind.VARIABLE, declaration);
        final SemanticType type = new SemanticType.Primitive(PrimitiveTypeName.INT);

        reference.setResolvedType(type);
        reference.setSymbol(symbol);

        assertThat(reference.name()).isEqualTo(COUNT);
        assertThat(reference.resolvedType()).contains(type);
        assertThat(reference.symbol()).contains(symbol);
    }

    @Test
    void visitorUsesTheConcreteNodeMethodAndFallsBackToVisitNode() {
        final IntLiteral literal = new IntLiteral(7L, RANGE);
        final AstVisitor<String> specializedVisitor =
                new AstVisitor<>() {
                    @Override
                    public String visitIntLiteral(final IntLiteral node) {
                        return "int:" + node.value();
                    }
                };
        final AstVisitor<String> fallbackVisitor =
                new AstVisitor<>() {
                    @Override
                    public String visitNode(final Node node) {
                        return node.kind().name();
                    }
                };

        assertThat(literal.accept(specializedVisitor)).isEqualTo("int:7");
        assertThat(new BreakStmt(RANGE).accept(fallbackVisitor)).isEqualTo("BREAK_STMT");
    }

    @Test
    void requiredNodeComponentsAreRejectedImmediately() {
        assertThatThrownBy(() -> new IntLiteral(1L, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new FunctionDecl("f", null, null, null, RANGE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new NameExpr(" ", RANGE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
