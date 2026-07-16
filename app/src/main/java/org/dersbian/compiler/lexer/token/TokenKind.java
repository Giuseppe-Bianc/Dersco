package org.dersbian.compiler.lexer.token;

import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Represents every possible token kind in the language.
 *
 * <p>The hierarchy is split into:
 *
 * <ul>
 *   <li>{@link Simple} - payload-free tokens (operators, keywords, delimiters, specials)
 *   <li>Records with payload - literals, identifiers
 * </ul>
 */
@SuppressWarnings("PMD.ShortVariable")
public sealed interface TokenKind
        permits TokenKind.Simple,
                TokenKind.KeywordBool,
                TokenKind.IdentifierAscii,
                TokenKind.IdentifierUnicode,
                TokenKind.Numeric,
                TokenKind.Binary,
                TokenKind.Octal,
                TokenKind.Hexadecimal,
                TokenKind.StringLiteral,
                TokenKind.CharLiteral {

    // ------------------------------------------------------------------
    // Interface methods
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} if this token represents a primitive type keyword (i8, u8, f32, bool,
     * etc.).
     *
     * @return {@code true} for all {@link Simple.TypeKeyword} variants, {@code false} otherwise
     */
    default boolean isType() {
        return this instanceof Simple.TypeKeyword;
    }

    // ------------------------------------------------------------------
    // Payload-free variants
    // ------------------------------------------------------------------

    /**
     * Sealed marker for all tokens that carry no associated data. The actual constants live in the
     * four nested enums: {@link Operator}, {@link Keyword}, {@link TypeKeyword}, {@link Delimiter}
     * and {@link Special}.
     */
    sealed interface Simple extends TokenKind
            permits Simple.Operator,
                    Simple.Keyword,
                    Simple.TypeKeyword,
                    Simple.Delimiter,
                    Simple.Special {

        /** Arithmetic, logical, relational and assignment operators. */
        enum Operator implements Simple {
            // Multi-character
            PLUS_EQUAL,
            MINUS_EQUAL,
            EQUAL_EQUAL,
            NOT_EQUAL,
            LESS_EQUAL,
            GREATER_EQUAL,
            PLUS_PLUS,
            MINUS_MINUS,
            OR_OR,
            AND_AND,
            AND_EQUAL,
            OR_EQUAL,
            BITWISE_NOT,
            SHIFT_LEFT_EQUAL,
            SHIFT_RIGHT_EQUAL,
            SHIFT_LEFT,
            SHIFT_RIGHT,
            PERCENT_EQUAL,
            XOR_EQUAL,
            STAR_EQUAL,
            SLASH_EQUAL,

            // Single-character
            PLUS,
            MINUS,
            STAR,
            SLASH,
            LESS,
            GREATER,
            NOT,
            XOR,
            PERCENT,
            OR,
            AND,
            EQUAL,
            COLON,
            COMMA,
            DOT;

            @SuppressWarnings("PMD.CyclomaticComplexity")
            @Override
            public String toString() {
                final String value =
                        switch (this) {
                            case PLUS -> "'+'";
                            case MINUS -> "'-'";
                            case STAR -> "'*'";
                            case SLASH -> "'/'";
                            case PLUS_EQUAL -> "'+='";
                            case MINUS_EQUAL -> "'-='";
                            case EQUAL_EQUAL -> "'=='";
                            case NOT_EQUAL -> "'!='";
                            case STAR_EQUAL -> "'*='";
                            case SLASH_EQUAL -> "'/='";
                            case LESS -> "'<'";
                            case GREATER -> "'>'";
                            case SHIFT_LEFT_EQUAL -> "'<<='";
                            case SHIFT_RIGHT_EQUAL -> "'>>='";
                            case LESS_EQUAL -> "'<='";
                            case GREATER_EQUAL -> "'>='";
                            case PLUS_PLUS -> "'++'";
                            case MINUS_MINUS -> "'--'";
                            case OR_OR -> "'||'";
                            case AND_AND -> "'&&'";
                            case AND_EQUAL -> "'&='";
                            case OR_EQUAL -> "'|='";
                            case SHIFT_LEFT -> "'<<'";
                            case SHIFT_RIGHT -> "'>>'";
                            case PERCENT_EQUAL -> "'%='";
                            case XOR_EQUAL -> "'^='";
                            case BITWISE_NOT -> "'~'";
                            case NOT -> "'!'";
                            case XOR -> "'^'";
                            case PERCENT -> "'%'";
                            case OR -> "'|'";
                            case AND -> "'&'";
                            case EQUAL -> "'='";
                            case COLON -> "':'";
                            case COMMA -> "','";
                            case DOT -> "'.'";
                        };
                return "Operator " + value;
            }
        }

        /** Language keywords (control flow, declarations). */
        enum Keyword implements Simple {
            FUN,
            IF,
            ELSE,
            RETURN,
            WHILE,
            FOR,
            MAIN,
            VAR,
            CONST,
            NULLPTR,
            BREAK,
            CONTINUE;

            @SuppressWarnings("PMD.CyclomaticComplexity")
            @Override
            public String toString() {
                final String value =
                        switch (this) {
                            case FUN -> "'fun'";
                            case IF -> "'if'";
                            case ELSE -> "'else'";
                            case RETURN -> "'return'";
                            case WHILE -> "'while'";
                            case FOR -> "'for'";
                            case MAIN -> "'main'";
                            case VAR -> "'var'";
                            case CONST -> "'const'";
                            case NULLPTR -> "'nullptr'";
                            case BREAK -> "'break'";
                            case CONTINUE -> "'continue'";
                        };
                return "Keyword " + value;
            }
        }

        /** Primitive type keywords. */
        enum TypeKeyword implements Simple {
            I8,
            I16,
            I32,
            I64,
            U8,
            U16,
            U32,
            U64,
            F32,
            F64,
            CHAR,
            STRING,
            BOOL;

            @SuppressWarnings("PMD.CyclomaticComplexity")
            @Override
            public String toString() {
                final String value =
                        switch (this) {
                            case I8 -> "'i8'";
                            case I16 -> "'i16'";
                            case I32 -> "'i32'";
                            case I64 -> "'i64'";
                            case U8 -> "'u8'";
                            case U16 -> "'u16'";
                            case U32 -> "'u32'";
                            case U64 -> "'u64'";
                            case F32 -> "'f32'";
                            case F64 -> "'f64'";
                            case CHAR -> "'char'";
                            case STRING -> "'string'";
                            case BOOL -> "'bool'";
                        };
                return "Type " + value;
            }
        }

        /** Delimiters: round, square and curly brackets. */
        enum Delimiter implements Simple {
            OPEN_PAREN,
            CLOSE_PAREN,
            OPEN_BRACKET,
            CLOSE_BRACKET,
            OPEN_BRACE,
            CLOSE_BRACE;

            @Override
            public String toString() {
                final String value =
                        switch (this) {
                            case OPEN_PAREN -> "'('";
                            case CLOSE_PAREN -> "')'";
                            case OPEN_BRACKET -> "'['";
                            case CLOSE_BRACKET -> "']'";
                            case OPEN_BRACE -> "'{'";
                            case CLOSE_BRACE -> "'}'";
                        };
                return "Delimiter " + value;
            }
        }

        /** Special tokens: semicolon, comments and end-of-file marker. */
        enum Special implements Simple {
            SEMICOLON,
            COMMENT,
            MULTILINE_COMMENT,
            /** End-of-file marker (never produced directly by the lexer). */
            EOF;

            @Override
            public String toString() {
                return switch (this) {
                    case SEMICOLON -> "';'";
                    case COMMENT -> "Comment";
                    case MULTILINE_COMMENT -> "Multiline Comment";
                    case EOF -> "EOF";
                };
            }
        }
    }

    // ------------------------------------------------------------------
    // Payload-carrying variants
    // ------------------------------------------------------------------

    /** Boolean literal ({@code true}/{@code false}). */
    record KeywordBool(boolean value) implements TokenKind {

        @Override
        public String toString() {
            return "boolean '" + value + "'";
        }
    }

    /** ASCII identifier (letters, digits, underscore). */
    record IdentifierAscii(String value) implements TokenKind {

        @Override
        public String toString() {
            return "Identifier '" + value + "'";
        }
    }

    /** Unicode identifier (supports international characters). */
    record IdentifierUnicode(String value) implements TokenKind {

        @Override
        public String toString() {
            return "Identifier '" + value + "'";
        }
    }

    /** Numeric literal (integers, floats, scientific notation, suffixes). */
    record Numeric(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Number '" + value + "'";
        }
    }

    /** Binary literal (e.g. {@code #b1010u}). */
    record Binary(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Binary '" + value + "'";
        }
    }

    /** Octal literal (e.g. {@code #o755}). */
    record Octal(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Octal '" + value + "'";
        }
    }

    /** Hexadecimal literal (e.g. {@code #xdeadbeefu}). */
    record Hexadecimal(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Hexadecimal '" + value + "'";
        }
    }

    /** String literal (content already stripped of surrounding quotes). */
    record StringLiteral(String value) implements TokenKind {

        @Override
        public String toString() {
            return "String literal \"" + value + "\"";
        }
    }

    /** Character literal (content already stripped of surrounding quotes). */
    record CharLiteral(String value) implements TokenKind {

        @Override
        public String toString() {
            return "Character literal '" + value + "'";
        }
    }
}
