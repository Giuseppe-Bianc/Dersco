package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.lexer.token.TokenKind.Simple.Operator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnitTestContainsTooManyAsserts"})
class BindingPowerTest {

    static Stream<Operator> assignmentOps() {
        return Stream.of(
                Operator.EQUAL,
                Operator.PLUS_EQUAL,
                Operator.MINUS_EQUAL,
                Operator.STAR_EQUAL,
                Operator.SLASH_EQUAL,
                Operator.PERCENT_EQUAL,
                Operator.AND_EQUAL,
                Operator.OR_EQUAL,
                Operator.XOR_EQUAL,
                Operator.SHIFT_LEFT_EQUAL,
                Operator.SHIFT_RIGHT_EQUAL);
    }

    @ParameterizedTest
    @MethodSource("assignmentOps")
    void assignmentOperatorsAreRightAssociative(final Operator op) {
        final Optional<BindingPower> bp = BindingPower.infix(op);
        assertThat(bp).isPresent();
        assertThat(bp.get().left()).isEqualTo(10);
        assertThat(bp.get().right()).isEqualTo(9);
    }

    @Test
    void logicalOrBindingPower() {
        assertThat(BindingPower.infix(Operator.OR_OR)).contains(new BindingPower(20, 21));
    }

    @Test
    void logicalAndBindingPower() {
        assertThat(BindingPower.infix(Operator.AND_AND)).contains(new BindingPower(30, 31));
    }

    @Test
    void bitwiseOrBindingPower() {
        assertThat(BindingPower.infix(Operator.OR)).contains(new BindingPower(40, 41));
    }

    @Test
    void bitwiseXorBindingPower() {
        assertThat(BindingPower.infix(Operator.XOR)).contains(new BindingPower(50, 51));
    }

    @Test
    void bitwiseAndBindingPower() {
        assertThat(BindingPower.infix(Operator.AND)).contains(new BindingPower(60, 61));
    }

    @Test
    void equalityOperatorsBindingPower() {
        assertThat(BindingPower.infix(Operator.EQUAL_EQUAL)).contains(new BindingPower(70, 71));
        assertThat(BindingPower.infix(Operator.NOT_EQUAL)).contains(new BindingPower(70, 71));
    }

    @Test
    void relationalOperatorsBindingPower() {
        assertThat(BindingPower.infix(Operator.LESS)).contains(new BindingPower(80, 81));
        assertThat(BindingPower.infix(Operator.LESS_EQUAL)).contains(new BindingPower(80, 81));
        assertThat(BindingPower.infix(Operator.GREATER)).contains(new BindingPower(80, 81));
        assertThat(BindingPower.infix(Operator.GREATER_EQUAL)).contains(new BindingPower(80, 81));
    }

    @Test
    void shiftOperatorsBindingPower() {
        assertThat(BindingPower.infix(Operator.SHIFT_LEFT)).contains(new BindingPower(90, 91));
        assertThat(BindingPower.infix(Operator.SHIFT_RIGHT)).contains(new BindingPower(90, 91));
    }

    @Test
    void additiveOperatorsBindingPower() {
        assertThat(BindingPower.infix(Operator.PLUS)).contains(new BindingPower(100, 101));
        assertThat(BindingPower.infix(Operator.MINUS)).contains(new BindingPower(100, 101));
    }

    @Test
    void multiplicativeOperatorsBindingPower() {
        assertThat(BindingPower.infix(Operator.STAR)).contains(new BindingPower(110, 111));
        assertThat(BindingPower.infix(Operator.SLASH)).contains(new BindingPower(110, 111));
        assertThat(BindingPower.infix(Operator.PERCENT)).contains(new BindingPower(110, 111));
    }

    @Test
    void postfixAndCallAndIndexBindingPower() {
        assertThat(BindingPower.infix(Operator.PLUS_PLUS)).contains(new BindingPower(130, 130));
        assertThat(BindingPower.infix(Operator.MINUS_MINUS)).contains(new BindingPower(130, 130));
        assertThat(BindingPower.infix(TokenKind.Simple.Delimiter.OPEN_PAREN))
                .contains(new BindingPower(130, 130));
        assertThat(BindingPower.infix(TokenKind.Simple.Delimiter.OPEN_BRACKET))
                .contains(new BindingPower(130, 130));
    }

    static Stream<Operator> prefixOps() {
        return Stream.of(
                Operator.MINUS,
                Operator.NOT,
                Operator.BITWISE_NOT,
                Operator.PLUS_PLUS,
                Operator.MINUS_MINUS);
    }

    @ParameterizedTest
    @MethodSource("prefixOps")
    void prefixOperatorsHaveRightBp120(final Operator op) {
        final OptionalInt bp = BindingPower.prefix(op);
        assertThat(bp).isPresent();
        assertThat(bp.getAsInt()).isEqualTo(120);
    }

    @Test
    void nonOperatorTokenHasNoInfixBp() {
        assertThat(BindingPower.infix(TokenKind.Simple.Keyword.FUN)).isEmpty();
    }

    @Test
    void nonPrefixTokenHasNoPrefixBp() {
        assertThat(BindingPower.prefix(TokenKind.Simple.Keyword.FUN)).isEmpty();
    }
}
