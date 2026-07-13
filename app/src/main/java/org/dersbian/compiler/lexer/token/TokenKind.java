package org.dersbian.compiler.lexer.token;

import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Rappresenta tutti i possibili tipi di token del linguaggio.
 *
 * <p>La gerarchia è suddivisa in:
 *
 * <ul>
 *   <li>{@link Simple} – token privi di payload (operatori, parole chiave, delimitatori, speciali)
 *   <li>Record con payload – letterali, identificatori
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
    // Metodi dell'interfaccia
    // ------------------------------------------------------------------

    /**
     * Verifica se il token rappresenta una parola chiave di tipo (i8, u8, f32, bool, …).
     *
     * @return {@code true} per tutte le varianti {@link Simple.TypeKeyword}, {@code false}
     *     altrimenti
     */
    default boolean isType() {
        return this instanceof Simple.TypeKeyword;
    }

    // ------------------------------------------------------------------
    // Varianti senza payload
    // ------------------------------------------------------------------

    /**
     * Marker sealed per tutti i token privi di dati associati. Le costanti effettive vivono nei
     * quattro enum annidati: {@link Operator}, {@link Keyword}, {@link TypeKeyword}, {@link
     * Delimiter} e {@link Special}.
     */
    sealed interface Simple extends TokenKind
            permits Simple.Operator,
                    Simple.Keyword,
                    Simple.TypeKeyword,
                    Simple.Delimiter,
                    Simple.Special {

        /** Operatori aritmetici, logici, relazionali e di assegnazione. */
        enum Operator implements Simple {
            // Multi-carattere
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

            // Singolo carattere
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

        /** Parole chiave del linguaggio (control flow, dichiarazioni). */
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

        /** Parole chiave di tipo primitivo. */
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

        /** Delimitatori: parentesi tonde, quadre e graffe. */
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

        /** Token speciali: punto e virgola, commenti e marcatore di fine file. */
        enum Special implements Simple {
            SEMICOLON,
            COMMENT,
            MULTILINE_COMMENT,
            /** Marcatore di fine file (mai prodotto direttamente dal lexer). */
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
    // Varianti con payload
    // ------------------------------------------------------------------

    /** Letterale booleano ({@code true}/{@code false}). */
    record KeywordBool(boolean value) implements TokenKind {

        @Override
        public String toString() {
            return "boolean '" + value + "'";
        }
    }

    /** Identificatore ASCII (lettere, cifre, underscore). */
    record IdentifierAscii(String value) implements TokenKind {

        @Override
        public String toString() {
            return "Identifier '" + value + "'";
        }
    }

    /** Identificatore Unicode (supporta caratteri internazionali). */
    record IdentifierUnicode(String value) implements TokenKind {

        @Override
        public String toString() {
            return "Identifier '" + value + "'";
        }
    }

    /** Letterale numerico (interi, float, notazione scientifica, suffissi). */
    record Numeric(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Number '" + value + "'";
        }
    }

    /** Letterale binario (es. {@code #b1010u}). */
    record Binary(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Binary '" + value + "'";
        }
    }

    /** Letterale ottale (es. {@code #o755}). */
    record Octal(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Octal '" + value + "'";
        }
    }

    /** Letterale esadecimale (es. {@code #xdeadbeefu}). */
    record Hexadecimal(INumber value) implements TokenKind {

        @Override
        public String toString() {
            return "Hexadecimal '" + value + "'";
        }
    }

    /** Letterale stringa (contenuto già privato delle virgolette). */
    record StringLiteral(String value) implements TokenKind {

        @Override
        public String toString() {
            return "String literal \"" + value + "\"";
        }
    }

    /** Letterale carattere (contenuto già privato degli apici). */
    record CharLiteral(String value) implements TokenKind {

        @Override
        public String toString() {
            return "Character literal '" + value + "'";
        }
    }
}
