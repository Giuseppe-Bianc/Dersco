package org.dersbian.compiler.syntax;

import java.util.Optional;
import java.util.OptionalInt;
import org.dersbian.compiler.lexer.token.TokenKind;

/**
 * Pratt parser binding powers for infix and prefix operators.
 *
 * <p>The left and right values define the binding power used when parsing the token's left and
 * right operands.
 *
 * @param left left binding power used by the current parse level
 * @param right right binding power used when parsing the operand
 */
record BindingPower(int left, int right) {

    /**
     * Returns the binding powers for a token kind handled by the infix parsing step.
     *
     * @param kind token kind
     * @return binding powers when {@code kind} is an infix, postfix, call, or index token; otherwise
     *     {@link Optional#empty()}
     */
    @SuppressWarnings({
        "PMD.CyclomaticComplexity",
        "PMD.CommentDefaultAccessModifier",
        "PMD.ShortVariable"
    })
    static Optional<BindingPower> infix(final TokenKind kind) {
        return switch (kind) {
            case TokenKind.Simple.Operator op ->
                    switch (op) {
                        case EQUAL,
                                PLUS_EQUAL,
                                MINUS_EQUAL,
                                STAR_EQUAL,
                                SLASH_EQUAL,
                                PERCENT_EQUAL,
                                AND_EQUAL,
                                OR_EQUAL,
                                XOR_EQUAL,
                                SHIFT_LEFT_EQUAL,
                                SHIFT_RIGHT_EQUAL ->
                                Optional.of(new BindingPower(10, 9));
                        case OR_OR -> Optional.of(new BindingPower(20, 21));
                        case AND_AND -> Optional.of(new BindingPower(30, 31));
                        case OR -> Optional.of(new BindingPower(40, 41));
                        case XOR -> Optional.of(new BindingPower(50, 51));
                        case AND -> Optional.of(new BindingPower(60, 61));
                        case EQUAL_EQUAL, NOT_EQUAL -> Optional.of(new BindingPower(70, 71));
                        case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL ->
                                Optional.of(new BindingPower(80, 81));
                        case SHIFT_LEFT, SHIFT_RIGHT -> Optional.of(new BindingPower(90, 91));
                        case PLUS, MINUS -> Optional.of(new BindingPower(100, 101));
                        case STAR, SLASH, PERCENT -> Optional.of(new BindingPower(110, 111));
                        case PLUS_PLUS, MINUS_MINUS -> Optional.of(new BindingPower(130, 130));
                        default -> Optional.empty();
                    };
            case TokenKind.Simple.Delimiter.OPEN_PAREN, TokenKind.Simple.Delimiter.OPEN_BRACKET ->
                    Optional.of(new BindingPower(130, 130));
            default -> Optional.empty();
        };
    }

    /**
     * Returns the right binding power used for prefix operators.
     *
     * @param kind token kind
     * @return right binding power when {@code kind} is a prefix operator; otherwise {@link
     *     OptionalInt#empty()}
     */
    @SuppressWarnings({
        "PMD.CommentDefaultAccessModifier",
        "PMD.ShortVariable",
        "PMD.TooFewBranchesForSwitch"
    })
    static OptionalInt prefix(final TokenKind kind) {
        return switch (kind) {
            case TokenKind.Simple.Operator op ->
                    switch (op) {
                        case MINUS, NOT, BITWISE_NOT, PLUS_PLUS, MINUS_MINUS -> OptionalInt.of(120);
                        default -> OptionalInt.empty();
                    };
            default -> OptionalInt.empty();
        };
    }
}
