package org.dersbian.compiler.syntax;

import java.util.Optional;
import java.util.OptionalInt;
import org.dersbian.compiler.lexer.token.TokenKind;

/**
 * Pratt parser binding powers for infix and prefix operators.
 *
 * @param left left binding power (minimum bp for caller to enter)
 * @param right right binding power (minimum bp for operand)
 */
record BindingPower(int left, int right) {

    /**
     * Infix (left-denotation) binding power for the given token kind.
     *
     * @param kind token kind
     * @return binding power when {@code kind} is an infix/postfix/call/index operator, else empty
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
     * Prefix (null-denotation) right binding power.
     *
     * @param kind token kind
     * @return right bp when {@code kind} is a prefix operator, else empty
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
