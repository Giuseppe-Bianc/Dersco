```java
package org.dersbian.compiler.lexer.token.number;

/** Represents a numeric literal value. */
@SuppressWarnings({"AvoidCommonTypeNames", "checkstyle:AbbreviationAsWordInName"})
public sealed interface INumber {

    /** Integer value. */
    record IntegerValue(long value, String suffix) implements INumber {

        @Override
        public String toString() {
            return suffix == null ? Long.toString(value) : value + suffix;
        }
    }

    /** Floating-point value. */
    record FloatingValue(double value, String suffix) implements INumber {

        @Override
        public String toString() {
            return suffix == null ? Double.toString(value) : value + suffix;
        }
    }
}

package org.dersbian.compiler.lexer.token.parser.numeric;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Utility class providing static parser methods for non-decimal (binary, octal, hexadecimal)
 * numeric literals.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BaseParsers {

    /**
     * Parses a binary literal (prefixed with {@code #b}) into an {@link INumber}.
     *
     * @param literal the binary literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseBinary(final String literal) {
        return parseWithRadix(literal, "#b", 2);
    }

    /**
     * Parses an octal literal (prefixed with {@code #o}) into an {@link INumber}.
     *
     * @param literal the octal literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseOctal(final String literal) {
        return parseWithRadix(literal, "#o", 8);
    }

    /**
     * Parses a hexadecimal literal (prefixed with {@code #x}) into an {@link INumber}.
     *
     * @param literal the hexadecimal literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseHex(final String literal) {
        return parseWithRadix(literal, "#x", 16);
    }

    private static INumber parseWithRadix(
            final String literal, final String prefix, final int radix) {
        String body = literal.substring(prefix.length());
        String suffix = null;
        if (!body.isEmpty()) {
            final char last = body.charAt(body.length() - 1);
            if (last == 'u' || last == 'U') {
                suffix = String.valueOf(last);
                body = body.substring(0, body.length() - 1);
            }
        }
        final long value = Long.parseLong(body, radix);
        return new INumber.IntegerValue(value, suffix);
    }
}

package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Utility class providing static methods to parse numeric literal strings into {@link INumber}
 * instances, handling integer and floating-point types with optional type suffixes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NumericParsers {

    /** Minimum body length required to check for two-character suffixes. */
    private static final int MIN_TAIL2_LENGTH = 2;

    /** Minimum body length required to check for three-character suffixes. */
    private static final int MIN_TAIL3_LENGTH = 3;

    /**
     * Parses a numeric literal string into an {@link INumber}, detecting integer or floating-point
     * type and optional type suffixes (e.g. {@code u}, {@code f}, {@code i32}).
     *
     * @param literal the numeric literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseNumber(final String literal) {
        final SuffixResult result = extractSuffix(literal);
        final String body = result.body();
        final String suffix = result.suffix();

        final boolean isFloating =
                body.contains(".")
                        || body.toLowerCase(Locale.ROOT).contains("e")
                        || (suffix != null && "fFdD".contains(suffix));

        return isFloating
                ? new INumber.FloatingValue(Double.parseDouble(body), suffix)
                : new INumber.IntegerValue(Long.parseLong(body), suffix);
    }

    /**
     * Attempts to strip a single-character type suffix from the body.
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult trySingleCharSuffix(final String body) {
        final char last = body.charAt(body.length() - 1);
        final SuffixResult found;
        if ("uUfFdD".indexOf(last) >= 0) {
            found = new SuffixResult(body.substring(0, body.length() - 1), String.valueOf(last));
        } else {
            found = null;
        }
        return found;
    }

    /**
     * Attempts to strip a three-character type suffix from the body (case-insensitive, matching the
     * pattern {@code [iIuU](?:8|16|32)}).
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult tryTail3Suffix(final String body) {
        final SuffixResult found;
        if (body.length() < MIN_TAIL3_LENGTH) {
            found = null;
        } else {
            final String tail3 = body.substring(body.length() - MIN_TAIL3_LENGTH);
            final String normalized = tail3.toLowerCase(Locale.ROOT);
            if ("i32".equals(normalized)
                    || "u32".equals(normalized)
                    || "i16".equals(normalized)
                    || "u16".equals(normalized)) {
                found =
                        new SuffixResult(
                                body.substring(0, body.length() - MIN_TAIL3_LENGTH), tail3);
            } else {
                found = null;
            }
        }
        return found;
    }

    /**
     * Attempts to strip a two-character type suffix from the body (case-insensitive).
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult tryTail2Suffix(final String body) {
        final SuffixResult found;
        if (body.length() < MIN_TAIL2_LENGTH) {
            found = null;
        } else {
            final String tail2 = body.substring(body.length() - MIN_TAIL2_LENGTH);
            final String normalized = tail2.toLowerCase(Locale.ROOT);
            if ("i8".equals(normalized) || "u8".equals(normalized)) {
                found =
                        new SuffixResult(
                                body.substring(0, body.length() - MIN_TAIL2_LENGTH), tail2);
            } else {
                found = null;
            }
        }
        return found;
    }

    /**
     * Attempts to strip a multi-character type suffix (two or three characters) from the body.
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult tryMultiCharSuffix(final String body) {
        final SuffixResult tail3 = tryTail3Suffix(body);
        final SuffixResult found;
        if (tail3 != null) {
            found = tail3;
        } else {
            found = tryTail2Suffix(body);
        }
        return found;
    }

    /**
     * Extracts an optional type suffix from the numeric literal, returning the stripped body and
     * the detected suffix (or {@code null} if none was found).
     *
     * @param literal the full numeric literal string
     * @return a {@link SuffixResult} containing the body and optional suffix
     */
    private static SuffixResult extractSuffix(final String literal) {
        final SuffixResult result;
        if (literal.isEmpty()) {
            result = new SuffixResult(literal, null);
        } else {
            result = extractSuffixFromNonEmpty(literal);
        }
        return result;
    }

    private static SuffixResult extractSuffixFromNonEmpty(final String literal) {
        final SuffixResult single = trySingleCharSuffix(literal);
        final SuffixResult result;
        if (single != null) {
            result = single;
        } else {
            final SuffixResult multi = tryMultiCharSuffix(literal);
            if (multi != null) {
                result = multi;
            } else {
                result = new SuffixResult(literal, null);
            }
        }
        return result;
    }

    /**
     * Immutable value type holding a numeric body string and its optional type suffix.
     *
     * @param body the numeric literal with any suffix stripped
     * @param suffix the detected type suffix, or {@code null} if none
     */
    private record SuffixResult(String body, String suffix) {}
}

package org.dersbian.compiler.lexer.token;

import java.nio.file.Path;
import java.util.Objects;

/** Identifies the source. The ID is valid for the whole compilation. */
public sealed interface SourceId
        permits SourceId.FilePath,
                SourceId.VirtualResource,
                SourceId.InMemoryModule,
                SourceId.Generated {

    /** Stable textual identifier of the source. */
    String identifier();

    /** Human-readable description, useful for logs and diagnostic messages. */
    default String describe() {
        return switch (this) {
            case FilePath(var path) -> "file: " + path;
            case VirtualResource(var uri) -> "virtual: " + uri;
            case InMemoryModule(var moduleName) -> "in-memory module: " + moduleName;
            case Generated(var description) -> "generated: " + description;
        };
    }

    /** Source corresponding to a file on the filesystem. */
    record FilePath(Path path) implements SourceId {
        public FilePath {
            Objects.requireNonNull(path, "path must not be null");
        }

        @Override
        public String identifier() {
            return path.toString();
        }
    }

    /** Source of a virtual resource (URI, JAR, URL). */
    record VirtualResource(String uri) implements SourceId {
        public VirtualResource {
            Objects.requireNonNull(uri, "uri must not be null");
            if (uri.isBlank()) {
                throw new IllegalArgumentException("uri must not be blank");
            }
        }

        @Override
        public String identifier() {
            return uri;
        }
    }

    /** Source of an in-memory module (REPL, eval). */
    record InMemoryModule(String moduleName) implements SourceId {
        public InMemoryModule {
            Objects.requireNonNull(moduleName, "moduleName must not be null");
            if (moduleName.isBlank()) {
                throw new IllegalArgumentException("moduleName must not be blank");
            }
        }

        @Override
        public String identifier() {
            return moduleName;
        }
    }

    /** Source generated by the compiler (macros, etc.). */
    record Generated(String description) implements SourceId {
        public Generated {
            Objects.requireNonNull(description, "description must not be null");
            if (description.isBlank()) {
                throw new IllegalArgumentException("description must not be blank");
            }
        }

        @Override
        public String identifier() {
            return "<generated:" + description + ">";
        }
    }
}

package org.dersbian.compiler.lexer.token;

import java.util.Objects;

/** Source position within a compilation unit. */
public record SourceLocation(
        int line, int column, long offset, int index, long utf8Offset, long codePointOffset)
        implements Comparable<SourceLocation> {

    /** Sentinel value for optional fields that have not been computed. */
    public static final long UNKNOWN = -1L;

    /** Minimum value for 1-based fields. */
    private static final int MIN_1_BASED = 1;

    /** Minimum value for 0-based fields. */
    private static final int MIN_0_BASED = 0;

    /** Compact constructor that validates all parameters. */
    public SourceLocation {
        if (line < MIN_1_BASED) {
            throw new IllegalArgumentException("line must be >= 1 (1-based), got: " + line);
        }
        if (column < MIN_1_BASED) {
            throw new IllegalArgumentException("column must be >= 1 (1-based), got: " + column);
        }
        if (offset < MIN_0_BASED) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
        if (index < MIN_0_BASED) {
            throw new IllegalArgumentException("index must be >= 0, got: " + index);
        }
        if (utf8Offset != UNKNOWN && utf8Offset < MIN_0_BASED) {
            throw new IllegalArgumentException("utf8Offset must be >= 0 or UNKNOWN");
        }
        if (codePointOffset != UNKNOWN && codePointOffset < MIN_0_BASED) {
            throw new IllegalArgumentException("codePointOffset must be >= 0 or UNKNOWN");
        }
    }

    /** Creates a minimal position with line, column and offset only. */
    public static SourceLocation create(final int line, final int column, final long offset) {
        return new SourceLocation(line, column, offset, Math.toIntExact(offset), UNKNOWN, UNKNOWN);
    }

    /** Creates a fully specified position with all offset variants. */
    public static SourceLocation create(
            final int line,
            final int column,
            final long offset,
            final int index,
            final long utf8Offset,
            final long codePointOffset) {
        return new SourceLocation(line, column, offset, index, utf8Offset, codePointOffset);
    }

    /** Returns a copy with the given UTF-8 byte offset. */
    public SourceLocation withUtf8Offset(final long newUtf8Offset) {
        return new SourceLocation(line, column, offset, index, newUtf8Offset, codePointOffset);
    }

    /** Returns a copy with the given code-point offset. */
    public SourceLocation withCodePointOffset(final long cpOffset) {
        return new SourceLocation(line, column, offset, index, utf8Offset, cpOffset);
    }

    /**
     * Verifica se l'offset UTF-8 è stato calcolato.
     *
     * @return true se l'offset UTF-8 è diverso da {@link #UNKNOWN}.
     */
    public boolean hasUtf8Offset() {
        return utf8Offset != UNKNOWN;
    }

    /**
     * Verifica se l'offset in code point è stato calcolato.
     *
     * @return true se l'offset in code point è diverso da {@link #UNKNOWN}.
     */
    public boolean hasCodePointOffset() {
        return codePointOffset != UNKNOWN;
    }

    @Override
    public int compareTo(final SourceLocation other) {
        Objects.requireNonNull(other, "other must not be null");
        return Long.compare(this.offset, other.offset);
    }

    @Override
    public String toString() {
        return "line %d:column %d".formatted(line, column);
    }
}

package org.dersbian.compiler.lexer.token;

import java.util.Objects;

/**
 * Extent of a token in the source text.
 *
 * @param start start position (inclusive).
 * @param end end position (exclusive).
 */
public record Span(SourceLocation start, SourceLocation end) {

    /** Compact constructor that validates start/end ordering. */
    public Span {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (end.offset() < start.offset()) {
            throw new IllegalArgumentException(
                    "end offset (%d) must not precede start offset (%d)"
                            .formatted(end.offset(), start.offset()));
        }
    }

    /**
     * Creates a new span.
     *
     * @param start start position
     * @param end end position
     * @return the new span
     */
    public static Span create(final SourceLocation start, final SourceLocation end) {
        return new Span(start, end);
    }

    /** Creates a zero-length span at the given location. */
    public static Span point(final SourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return new Span(location, location);
    }

    /** Length in UTF-16 code units (consistent with {@link String#length()}). */
    public long length() {
        return end.offset() - start.offset();
    }

    /**
     * Verifica se lo span ha lunghezza zero.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return length() == 0L;
    }

    /**
     * Verifica se lo span si estende su più righe.
     *
     * @return {@code true} if multiline
     */
    public boolean isMultiline() {
        return start.line() != end.line();
    }

    /** Verifica se la posizione ricade nell'intervallo [start, end). */
    public boolean contains(final SourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return location.offset() >= start.offset() && location.offset() < end.offset();
    }

    /** Verifica se due span condividono almeno un carattere. */
    public boolean overlaps(final Span other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.start.offset() < other.end.offset() && other.start.offset() < this.end.offset();
    }

    /** Restituisce lo span minimo che contiene entrambi gli span. */
    public Span merge(final Span other) {
        Objects.requireNonNull(other, "other must not be null");
        final SourceLocation mergedStart =
                this.start.offset() <= other.start.offset() ? this.start : other.start;
        final SourceLocation mergedEnd =
                this.end.offset() >= other.end.offset() ? this.end : other.end;
        return new Span(mergedStart, mergedEnd);
    }

    /** Estrae il testo dallo span. */
    public String extractFrom(final CharSequence source) {
        Objects.requireNonNull(source, "source must not be null");
        return source.subSequence(Math.toIntExact(start.offset()), Math.toIntExact(end.offset()))
                .toString();
    }

    @Override
    public String toString() {
        final String result;
        if (this.isEmpty()) {
            result = start.toString();
        } else {
            result = "%s-%s".formatted(start, end);
        }
        return result;
    }
}

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

package org.dersbian.compiler.lexer.token;

import java.util.Comparator;
import org.dersbian.util.PathUtils;

/**
 * Fundamental lexical unit produced by the lexer.
 *
 * @param sourceId stable identifier of the source.
 * @param type point-like lexical kind of the token.
 * @param span extent in the source.
 */
public record Token(SourceId sourceId, TokenKind type, Span span) {

    /** Comparator by position. Not consistent with equals. */
    public static final Comparator<Token> BY_POSITION =
            Comparator.comparing(token -> token.span().start());

    /** Creates a token from an already built span. */
    public static Token create(final SourceId sourceId, final TokenKind type, final Span span) {
        return new Token(sourceId, type, span);
    }

    /** Creates a token from explicit start and end positions. */
    public static Token create(
            final SourceId sourceId,
            final TokenKind type,
            final SourceLocation start,
            final SourceLocation end) {
        return new Token(sourceId, type, Span.create(start, end));
    }

    /** Creates a synthetic end-of-source (EOF) token, with zero length. */
    public static Token eof(final SourceId sourceId, final SourceLocation location) {
        return new Token(sourceId, TokenKind.Simple.Special.EOF, Span.point(location));
    }

    /** Checks whether the token is of the given kind. */
    public boolean isType(final TokenKind candidate) {
        return type.equals(candidate);
    }

    /** Whether the token comes from an automatically generated source. */
    public boolean isSynthetic() {
        return sourceId instanceof SourceId.Generated;
    }

    @Override
    public String toString() {
        final String result;
        if (sourceId instanceof SourceId.FilePath(var path)) {
            result = PathUtils.truncatePath(path, 2);
        } else {
            result = sourceId.identifier();
        }
        return "%s %s:%s".formatted(type, result, span);
    }
}

package org.dersbian.compiler.lexer;

import java.util.Map;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.lexer.token.TokenKind;

/**
 * Static, stateless helpers for classifying individual Unicode code points and resolving whole
 * identifier lexemes. Nothing here touches lexer state; every method is a pure function of its
 * arguments, which makes each one testable without constructing a {@link Lexer}.
 */
public final class CodePoints {

    /** Static, immutable map of all keywords and type-keywords of the language. */
    private static final Map<String, TokenKind.Simple> KEYWORDS =
            Map.ofEntries(
                    Map.entry("fun", TokenKind.Simple.Keyword.FUN),
                    Map.entry("if", TokenKind.Simple.Keyword.IF),
                    Map.entry("else", TokenKind.Simple.Keyword.ELSE),
                    Map.entry("return", TokenKind.Simple.Keyword.RETURN),
                    Map.entry("while", TokenKind.Simple.Keyword.WHILE),
                    Map.entry("for", TokenKind.Simple.Keyword.FOR),
                    Map.entry("main", TokenKind.Simple.Keyword.MAIN),
                    Map.entry("var", TokenKind.Simple.Keyword.VAR),
                    Map.entry("const", TokenKind.Simple.Keyword.CONST),
                    Map.entry("nullptr", TokenKind.Simple.Keyword.NULLPTR),
                    Map.entry("break", TokenKind.Simple.Keyword.BREAK),
                    Map.entry("continue", TokenKind.Simple.Keyword.CONTINUE),
                    Map.entry("i8", TokenKind.Simple.TypeKeyword.I8),
                    Map.entry("i16", TokenKind.Simple.TypeKeyword.I16),
                    Map.entry("i32", TokenKind.Simple.TypeKeyword.I32),
                    Map.entry("i64", TokenKind.Simple.TypeKeyword.I64),
                    Map.entry("u8", TokenKind.Simple.TypeKeyword.U8),
                    Map.entry("u16", TokenKind.Simple.TypeKeyword.U16),
                    Map.entry("u32", TokenKind.Simple.TypeKeyword.U32),
                    Map.entry("u64", TokenKind.Simple.TypeKeyword.U64),
                    Map.entry("f32", TokenKind.Simple.TypeKeyword.F32),
                    Map.entry("f64", TokenKind.Simple.TypeKeyword.F64),
                    Map.entry("char", TokenKind.Simple.TypeKeyword.CHAR),
                    Map.entry("string", TokenKind.Simple.TypeKeyword.STRING),
                    Map.entry("bool", TokenKind.Simple.TypeKeyword.BOOL));

    private CodePoints() {
        throw new AssertionError("no instances");
    }

    /**
     * Removes a leading UTF-8 byte-order mark ({@code U+FEFF}), if present. Some editors write it
     * at the start of UTF-8 encoded files; if left untouched it would surface as a spurious invalid
     * character at position zero.
     */
    public static String stripByteOrderMark(final String text) {
        final String result;
        if (!text.isEmpty() && text.codePointAt(0) == Constants.BYTE_ORDER_MARK) {
            result = text.substring(Character.charCount(Constants.BYTE_ORDER_MARK));
        } else {
            result = text;
        }
        return result;
    }

    /**
     * Computes how many bytes the given Unicode code point would occupy when encoded as UTF-8. This
     * lets the cursor's UTF-8 offset track the real byte offset in the original source file, even
     * though the in-memory representation is a Java {@code String} (UTF-16).
     */
    public static int utf8ByteLength(final int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid Unicode code point: " + codePoint);
        }
        final int result;
        if (codePoint < Constants.UTF8_ONE_BYTE_LIMIT) {
            result = Constants.UTF8_ONE_BYTE_LENGTH;
        } else if (codePoint < Constants.UTF8_TWO_BYTE_LIMIT) {
            result = Constants.UTF8_TWO_BYTE_LENGTH;
        } else if (codePoint < Constants.UTF8_THREE_BYTE_LIMIT) {
            result = Constants.UTF8_THREE_BYTE_LENGTH;
        } else {
            result = Constants.UTF8_FOUR_BYTE_LENGTH;
        }
        return result;
    }

    /**
     * Returns {@code true} if the code point falls within the ASCII range (a single UTF-8 byte).
     */
    public static boolean isAscii(final int codePoint) {
        return Character.isValidCodePoint(codePoint) && codePoint < Constants.UTF8_ONE_BYTE_LIMIT;
    }

    /**
     * Returns {@code true} if the given code point is considered whitespace and should be skipped
     * between tokens. Combines {@link Character#isWhitespace(int)} with {@link
     * Character#isSpaceChar(int)} so that any whitespace-looking byte sequence from a UTF-8 source
     * file is skipped regardless of which category it falls under.
     */
    public static boolean isWhitespaceCodePoint(final int codePoint) {
        return codePoint != -1
                && (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint));
    }

    /**
     * Returns {@code true} if the code point can start an identifier: a Unicode letter (according
     * to {@link Character#isUnicodeIdentifierStart(int)}) or an underscore {@code '_'}.
     */
    public static boolean isIdentifierStart(final int codePoint) {
        return codePoint != -1
                && (codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint));
    }

    /**
     * Returns {@code true} if the code point can continue an already-started identifier: letters,
     * digits and other Unicode "identifier part" characters (according to {@link
     * Character#isUnicodeIdentifierPart(int)}), in addition to the underscore {@code '_'}.
     */
    public static boolean isIdentifierPart(final int codePoint) {
        return codePoint != -1
                && (codePoint == '_' || Character.isUnicodeIdentifierPart(codePoint));
    }

    /**
     * Determines the correct {@link TokenKind} for an already fully-scanned lexeme: a boolean
     * literal, a keyword, a type-keyword, or an ASCII or Unicode identifier.
     */
    public static TokenKind resolveIdentifierKind(final String value, final boolean asciiOnly) {
        final TokenKind kind;
        if (Constants.TRUE_LITERAL.equals(value)) {
            kind = new TokenKind.KeywordBool(true);
        } else if (Constants.FALSE_LITERAL.equals(value)) {
            kind = new TokenKind.KeywordBool(false);
        } else if (KEYWORDS.containsKey(value)) {
            kind = KEYWORDS.get(value);
        } else if (asciiOnly) {
            kind = new TokenKind.IdentifierAscii(value);
        } else {
            kind = new TokenKind.IdentifierUnicode(value);
        }
        return kind;
    }
}

package org.dersbian.compiler.lexer;

import org.dersbian.compiler.Constants;
import org.dersbian.compiler.lexer.token.SourceLocation;

/**
 * Owns the mutable scanning state for one source text: the UTF-16 cursor position, the
 * corresponding UTF-8 byte offset, the code point count, and the current line/column. Every method
 * here is about moving through the text or reading around the current position; nothing here knows
 * what a token is.
 */
public final class SourceCursor {

    /** The full source text being scanned, indexed by UTF-16 code unit. */
    private final String source;

    /**
     * Cached length of {@link #source} in UTF-16 code units, avoids repeated {@link
     * String#length()} calls.
     */
    private final int length;

    /**
     * Current absolute offset into the source text, expressed in UTF-8 bytes. This mirrors the
     * on-disk byte offset of the original UTF-8 encoded file, unlike {@link #position} which counts
     * UTF-16 code units.
     */
    private long utf8Offset;

    /** Current absolute offset into the source text, expressed in UTF-16 code units. */
    private int position;

    /**
     * Number of Unicode code points consumed so far. Differs from {@link #position} whenever the
     * source contains characters outside the Basic Multilingual Plane (encoded as surrogate pairs,
     * i.e. two UTF-16 code units for a single code point).
     */
    private long codePointOffset;

    /** Current 1-based line number in the source text. */
    private int lineNumber = 1;

    /** Current 1-based column number in the source text. */
    private int columnNumber = 1;

    /**
     * Creates a cursor positioned at the start of {@code source}.
     *
     * @param source the text to scan; must not be {@code null}
     */
    public SourceCursor(final String source) {
        this.source = source;
        this.length = source.length();
    }

    /** Returns {@code true} if the cursor has consumed the entire source text. */
    public boolean isAtEnd() {
        return position >= length;
    }

    /**
     * Decodes the code point starting at the given UTF-16 index, or {@code -1} if {@code index} is
     * at or past the end of {@link #source}. Centralizes the bounds check so that callers needing
     * to look more than one code point ahead can reuse an already-decoded code point instead of
     * re-decoding the same position, as {@link #match(int, int)} does.
     */
    private int codePointAt(final int index) {
        return index < length ? source.codePointAt(index) : -1;
    }

    /**
     * Returns the Unicode code point at the current position without consuming it, or {@code -1} if
     * the end of the source has been reached.
     */
    public int peekCodePoint() {
        return codePointAt(position);
    }

    /** Returns the Unicode code point immediately after the current one, or {@code -1} at end. */
    public int peekNextCodePoint() {
        final int codePoint = peekCodePoint();
        return codePoint == -1 ? -1 : codePointAt(position + Character.charCount(codePoint));
    }

    /**
     * Consumes and returns the Unicode code point at the current position, correctly advancing past
     * surrogate pairs, and updates line/column tracking along with the UTF-8 byte offset and code
     * point count. Line terminators {@code \n}, {@code \r} and {@code \r\n} are all normalized to a
     * single line break.
     *
     * <h2>Surrogate-pair handling</h2>
     *
     * <p>UTF-16 encodes code points above {@code U+FFFF} as a pair of {@code char} units: a high
     * surrogate ({@code 0xD800}..{@code 0xDBFF}) followed by a low surrogate ({@code
     * 0xDC00}..{@code 0xDFFF}). The Java {@link String} type stores text as a sequence of UTF-16
     * code units, so a single visible character can occupy two {@code char} slots.
     *
     * <p>This method is the single point that crosses that boundary. It must:
     *
     * <ul>
     *   <li>Read the code point via {@link String#codePointAt(int)}, which decodes a lone BMP code
     *       unit as itself and a high surrogate followed by a low surrogate as a single
     *       supplementary code point.
     *   <li>Advance {@link #position} by {@link Character#charCount(int)}, which is {@code 1} for
     *       BMP code points and {@code 2} for supplementary code points. Never advance by {@code 1}
     *       unconditionally; doing so would split a surrogate pair and leave {@code position}
     *       pointing at the low surrogate half on the next call.
     *   <li>Increment {@link #codePointOffset} and {@link #utf8Offset} by <em>one</em> each (code
     *       points and UTF-8 bytes are counted per logical character, not per UTF-16 code unit).
     *       The UTF-8 byte length comes from {@link CodePoints#utf8ByteLength(int)}.
     * </ul>
     *
     * <p>Malformed input (an unpaired high or low surrogate) is treated by {@link
     * String#codePointAt(int)} as a single code-unit value, so the cursor still moves forward, but
     * the resulting {@link SourceLocation} will reference an invalid code point. The lexer is
     * expected to surface this through {@link ErrorCode#E0001} at the call site; this method does
     * not throw.
     *
     * @return the consumed code point, or {@code -1} at end of source.
     */
    public int advance() {
        final int codePoint = peekCodePoint();
        if (codePoint != -1) {
            position += Character.charCount(codePoint);
            utf8Offset += CodePoints.utf8ByteLength(codePoint);
            codePointOffset++;
            switch (codePoint) {
                case Constants.LINE_FEED,
                        Constants.LINE_SEPARATOR,
                        Constants.PARAGRAPH_SEPARATOR -> {
                    lineNumber++;
                    columnNumber = 1;
                }
                case Constants.CARRIAGE_RETURN -> {
                    if (peekCodePoint() != Constants.LINE_FEED) {
                        lineNumber++;
                        columnNumber = 1;
                    }
                }
                default -> columnNumber++;
            }
        }
        return codePoint;
    }

    /**
     * Consumes the current code point if it equals {@code expected}; otherwise consumes nothing.
     */
    public boolean match(final int expected) {
        final boolean matched = peekCodePoint() == expected;
        if (matched) {
            advance();
        }
        return matched;
    }

    /**
     * Consumes the current and next code point atomically if they equal {@code first} and {@code
     * second} respectively; otherwise consumes nothing. Useful for two-character sequences that
     * must be recognized before deciding how to treat the first character alone, e.g. {@code "//"}
     * versus a lone {@code '/'}.
     */
    public boolean match(final int first, final int second) {
        final int codePoint = peekCodePoint();
        final boolean matched =
                codePoint == first
                        && codePointAt(position + Character.charCount(codePoint)) == second;
        if (matched) {
            advance();
            advance();
        }
        return matched;
    }

    /** Builds the {@link SourceLocation} for the current cursor position. */
    public SourceLocation currentLocation() {
        return SourceLocation.create(
                lineNumber, columnNumber, position, position, utf8Offset, codePointOffset);
    }
}

package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.dersbian.compiler.lexer.token.parser.numeric.BaseParsers;
import org.dersbian.compiler.lexer.token.parser.numeric.NumericParsers;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
public class Lexer {

    /** Tracker used to map source positions to line numbers. */
    @Getter private final LineTracker lineTracker;

    /** Collected tokens produced by the lexer. */
    private final List<Token> tokens;

    /** Collected lexical and syntax errors. */
    private final List<CompileError> errors = new ArrayList<>();

    /** Stable identifier of the source being lexed. */
    private final SourceId sourceId;

    /** Cursor over the source text; owns all position, line and column bookkeeping. */
    private final SourceCursor cursor;

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        final String normalized = CodePoints.stripByteOrderMark(source);
        this.tokens = new ArrayList<>(estimatedTokenCapacity(normalized));
        this.cursor = new SourceCursor(normalized);
        this.lineTracker = LineTracker.fromText(normalized);
    }

    private static int estimatedTokenCapacity(final String source) {
        return (source.length() / Constants.ESTIMATED_CHARS_PER_TOKEN) + 1;
    }

    /**
     * Tokenizes the source text and returns the resulting tokens and any errors encountered.
     *
     * @return the result containing the produced tokens and collected errors.
     */
    public LexerResult tokenize() {
        while (!cursor.isAtEnd()) {
            skipWhitespace();
            if (cursor.isAtEnd()) {
                break;
            }
            final int codePoint = cursor.peekCodePoint();
            switch (codePoint) {
                case '+',
                        '-',
                        '*',
                        '=',
                        '!',
                        '<',
                        '>',
                        '|',
                        '&',
                        '%',
                        '^',
                        ':',
                        ',',
                        '.',
                        '~',
                        ';' ->
                        scanOperatorOrNumber();
                case '/' -> scanSlashOrComment();
                case '(', '[', '{', ')', ']', '}' -> scanDelimiter();
                case '#' -> scanRadixLiteral();
                case '"' -> scanStringLiteral();
                case '\'' -> scanCharLiteral();
                default -> {
                    if (Character.isDigit(codePoint)) {
                        scanNumber();
                    } else if (CodePoints.isIdentifierStart(codePoint)) {
                        scanIdentifierOrKeyword();
                    } else {
                        final SourceLocation start = cursor.currentLocation();
                        final int ecodePoint = cursor.advance();
                        reportError(
                                ErrorCode.E0001,
                                start,
                                "Unrecognized character: '" + Character.toString(ecodePoint) + "'",
                                null);
                    }
                }
            }
        }
        tokens.add(Token.eof(sourceId, cursor.currentLocation()));
        return new LexerResult(tokens, errors);
    }

    private void scanDelimiter() {
        final SourceLocation start = cursor.currentLocation();
        final int codePoint = cursor.advance();
        final TokenKind.Simple.Delimiter kind =
                switch (codePoint) {
                    case '(' -> TokenKind.Simple.Delimiter.OPEN_PAREN;
                    case ')' -> TokenKind.Simple.Delimiter.CLOSE_PAREN;
                    case '[' -> TokenKind.Simple.Delimiter.OPEN_BRACKET;
                    case ']' -> TokenKind.Simple.Delimiter.CLOSE_BRACKET;
                    case '{' -> TokenKind.Simple.Delimiter.OPEN_BRACE;
                    case '}' -> TokenKind.Simple.Delimiter.CLOSE_BRACE;
                    default ->
                            throw new IllegalStateException(
                                    "Unreachable: unexpected delimiter code point '"
                                            + codePoint
                                            + "'");
                };
        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, kind, span));
    }

    /**
     * Consumes consecutive whitespace code points without producing any tokens for them. Line,
     * column, UTF-8 offset and code point offset bookkeeping is delegated to {@link
     * SourceCursor#advance()}, so it stays correct for multi-byte whitespace characters too.
     */
    private void skipWhitespace() {
        while (!cursor.isAtEnd() && CodePoints.isWhitespaceCodePoint(cursor.peekCodePoint())) {
            cursor.advance();
        }
    }

    /**
     * Scans a full identifier, type-keyword, keyword or boolean literal starting at the current
     * position, and appends the resulting token to {@link #tokens}. Code points are consumed one at
     * a time via {@link SourceCursor#advance()}, so multi-byte / surrogate-pair characters (e.g.
     * accented letters, CJK ideographs, emoji used as identifiers if allowed by {@link
     * Character#isUnicodeIdentifierPart(int)}) are never split.
     */
    private void scanIdentifierOrKeyword() {
        final StringBuilder builder = new StringBuilder();
        boolean asciiOnly = true;

        final SourceLocation start = cursor.currentLocation();

        int codePoint = cursor.peekCodePoint();
        while (CodePoints.isIdentifierPart(codePoint)) {
            if (!CodePoints.isAscii(codePoint)) {
                asciiOnly = false;
            }
            builder.appendCodePoint(codePoint);
            cursor.advance();
            codePoint = cursor.peekCodePoint();
        }

        final String value = builder.toString();
        final Span span = Span.create(start, cursor.currentLocation());
        final TokenKind kind = CodePoints.resolveIdentifierKind(value, asciiOnly);
        tokens.add(Token.create(sourceId, kind, span));
    }

    /**
     * Scans a single operator token (single- or multi-character) starting at the current position
     * and appends the resulting token to {@link #tokens}.
     *
     * <p>Precondition: {@link SourceCursor#peekCodePoint()} must correspond to one of the
     * characters handled by the dedicated {@code case} branch for operators in {@link #tokenize()}.
     */
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.OnlyOneReturn"})
    private void scanOperatorOrNumber() {
        final SourceLocation start = cursor.currentLocation();
        final int codePoint = cursor.peekCodePoint();

        // Handle '.' specially - could be DOT operator or start of numeric literal like .5
        if (codePoint == '.') {
            cursor.advance();
            if (!cursor.isAtEnd() && Character.isDigit(cursor.peekCodePoint())) {
                // This is a numeric literal starting with '.' (e.g., .5, .123e10)
                scanNumericLiteralFromFraction(start);
                return;
            }
            // Just a DOT operator
            final Span span = Span.create(start, cursor.currentLocation());
            tokens.add(Token.create(sourceId, TokenKind.Simple.Operator.DOT, span));
            return;
        }

        final TokenKind.Simple kind = consumeOperatorKind();
        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, kind, span));
    }

    private void scanNumber() {
        final SourceLocation start = cursor.currentLocation();
        final StringBuilder literal = new StringBuilder();

        consumeDigits(literal);
        consumeOptionalFraction(literal);
        finishNumericLiteral(start, literal);
    }

    /**
     * Scans a numeric literal that started with '.', where the '.' has already been consumed.
     * Handles patterns like .5, .123, .5e10, .5f, etc.
     */
    private void scanNumericLiteralFromFraction(final SourceLocation start) {
        final StringBuilder literal = new StringBuilder(".");

        // At least one digit is guaranteed by the caller
        consumeDigits(literal);
        finishNumericLiteral(start, literal);
    }

    /** Scans an optional numeric suffix into the provided builder. */
    @SuppressWarnings("PMD.ShortVariable")
    private void scanNumericSuffix(final StringBuilder literal) {
        if (cursor.isAtEnd()) {
            return;
        }

        final int cp = cursor.peekCodePoint();

        // Single-char suffixes: u, U, f, F, d, D
        // Multi-char suffixes: i8, i16, i32, u8, u16, u32 (case-insensitive)
        if (cp == 'i' || cp == 'I' || cp == 'u' || cp == 'U') {
            literal.appendCodePoint(cp);
            cursor.advance();

            if (!cursor.isAtEnd() && Character.isDigit(cursor.peekCodePoint())) {
                scanSuffixDigits(literal);
            }
        } else if ("fFdD".indexOf(cp) >= 0) {
            literal.appendCodePoint(cp);
            cursor.advance();
        }
    }

    private void scanSuffixDigits(final StringBuilder literal) {
        // Consume 1-2 digits for type width (8, 16, 32)
        int digitCount = 0;
        while (!cursor.isAtEnd() && Character.isDigit(cursor.peekCodePoint()) && digitCount < 2) {
            literal.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
            digitCount++;
        }
    }

    private void consumeDigits(final StringBuilder literal) {
        while (!cursor.isAtEnd() && Character.isDigit(cursor.peekCodePoint())) {
            literal.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }
    }

    private void consumeOptionalFraction(final StringBuilder literal) {
        if (!cursor.isAtEnd() && cursor.peekCodePoint() == '.') {
            literal.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();

            consumeDigits(literal);
        }
    }

    private void consumeOptionalExponent(final StringBuilder literal) {
        if (!cursor.isAtEnd() && (cursor.peekCodePoint() == 'e' || cursor.peekCodePoint() == 'E')) {
            literal.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();

            if (!cursor.isAtEnd()
                    && (cursor.peekCodePoint() == '+' || cursor.peekCodePoint() == '-')) {
                literal.appendCodePoint(cursor.peekCodePoint());
                cursor.advance();
            }

            consumeDigits(literal);
        }
    }

    private void finishNumericLiteral(final SourceLocation start, final StringBuilder literal) {
        consumeOptionalExponent(literal);
        scanNumericSuffix(literal);

        final Span span = Span.create(start, cursor.currentLocation());
        final INumber value = NumericParsers.parseNumber(literal.toString());
        tokens.add(Token.create(sourceId, new TokenKind.Numeric(value), span));
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private TokenKind.Simple consumeOperatorKind() {
        final int codePoint = cursor.advance();

        return switch (codePoint) {
            case '+' ->
                    cursor.match('+')
                            ? TokenKind.Simple.Operator.PLUS_PLUS
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.PLUS_EQUAL
                                    : TokenKind.Simple.Operator.PLUS;
            case '-' ->
                    cursor.match('-')
                            ? TokenKind.Simple.Operator.MINUS_MINUS
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.MINUS_EQUAL
                                    : TokenKind.Simple.Operator.MINUS;
            case '*' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.STAR_EQUAL
                            : TokenKind.Simple.Operator.STAR;
            case '=' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.EQUAL_EQUAL
                            : TokenKind.Simple.Operator.EQUAL;
            case '!' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.NOT_EQUAL
                            : TokenKind.Simple.Operator.NOT;
            case '<' ->
                    cursor.match('<', '=')
                            ? TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL
                            : cursor.match('<')
                                    ? TokenKind.Simple.Operator.SHIFT_LEFT
                                    : cursor.match('=')
                                            ? TokenKind.Simple.Operator.LESS_EQUAL
                                            : TokenKind.Simple.Operator.LESS;
            case '>' ->
                    cursor.match('>', '=')
                            ? TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL
                            : cursor.match('>')
                                    ? TokenKind.Simple.Operator.SHIFT_RIGHT
                                    : cursor.match('=')
                                            ? TokenKind.Simple.Operator.GREATER_EQUAL
                                            : TokenKind.Simple.Operator.GREATER;
            case '|' ->
                    cursor.match('|')
                            ? TokenKind.Simple.Operator.OR_OR
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.OR_EQUAL
                                    : TokenKind.Simple.Operator.OR;
            case '&' ->
                    cursor.match('&')
                            ? TokenKind.Simple.Operator.AND_AND
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.AND_EQUAL
                                    : TokenKind.Simple.Operator.AND;
            case '%' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.PERCENT_EQUAL
                            : TokenKind.Simple.Operator.PERCENT;
            case '^' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.XOR_EQUAL
                            : TokenKind.Simple.Operator.XOR;
            case ':' -> TokenKind.Simple.Operator.COLON;
            case ',' -> TokenKind.Simple.Operator.COMMA;
            case '~' -> TokenKind.Simple.Operator.BITWISE_NOT;
            case ';' -> TokenKind.Simple.Special.SEMICOLON;
            default ->
                    throw new IllegalStateException(
                            "Unreachable: unexpected operator start code point '"
                                    + codePoint
                                    + "'");
        };
    }

    // ------------------------------------------------------------------
    // Comments
    // ------------------------------------------------------------------

    /**
     * Determines whether the {@code '/'} under the cursor starts a line comment ({@code //}), a
     * multi-line comment ({@code /*} ... {@code *}&#47;), the {@code /=} operator, or the plain
     * {@code /} operator, and dispatches to the appropriate scanning routine.
     *
     * <p>Precondition: {@link SourceCursor#peekCodePoint()} must be {@code '/'}.
     */
    private void scanSlashOrComment() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume the first '/'

        if (cursor.match('/')) {
            scanLineComment(start);
        } else if (cursor.match('*')) {
            scanMultilineComment(start);
        } else {
            final TokenKind.Simple.Operator kind =
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.SLASH_EQUAL
                            : TokenKind.Simple.Operator.SLASH;
            final Span span = Span.create(start, cursor.currentLocation());
            tokens.add(Token.create(sourceId, kind, span));
        }
    }

    /**
     * Scans a single-line comment ({@code // ...}) starting right after the opening {@code //} has
     * already been consumed by the caller. Consumes code points until a line terminator (excluded)
     * or the end of the source is reached, then appends a {@link TokenKind.Simple.Special#COMMENT}
     * token spanning from {@code start} to the current position.
     *
     * @param start the location of the first {@code '/'} of the comment
     */
    private void scanLineComment(final SourceLocation start) {
        while (!cursor.isAtEnd() && !isLineTerminator(cursor.peekCodePoint())) {
            cursor.advance();
        }
        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, TokenKind.Simple.Special.COMMENT, span));
    }

    /**
     * Scans a multi-line comment ({@code /*} ... {@code *}&#47;) starting right after the opening
     * {@code /*} has already been consumed by the caller. Consumes code points (including line
     * terminators, so multi-line comments may span several source lines) until the closing {@code
     * *}&#47; is found or the end of the source is reached.
     *
     * <p>Nesting is not supported: the first {@code *}&#47; sequence encountered closes the
     * comment, matching the semantics described by {@link ErrorCode#E0008}.
     *
     * <p>If the end of the source is reached before a closing {@code *}&#47; is found, {@link
     * ErrorCode#E0008} is reported, but a {@link TokenKind.Simple.Special#MULTILINE_COMMENT} token
     * is still produced (spanning up to the end of the source) to allow the parser to recover.
     *
     * @param start the location of the first {@code '/'} of the comment
     */
    private void scanMultilineComment(final SourceLocation start) {
        boolean terminated = false;

        while (!cursor.isAtEnd()) {
            if (cursor.peekCodePoint() == Constants.CHAR_ASTERISK) {
                cursor.advance();
                if (cursor.match('/')) {
                    terminated = true;
                    break;
                }
            } else {
                cursor.advance();
            }
        }

        if (!terminated) {
            reportError(ErrorCode.E0008, start, "Unterminated multi-line comment.", null);
        }

        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, TokenKind.Simple.Special.MULTILINE_COMMENT, span));
    }

    private void scanRadixLiteral() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume '#'

        final char prefixLetter = radixPrefixLetter(cursor.peekCodePoint());
        if (prefixLetter == 0) {
            reportError(
                    ErrorCode.E0001,
                    start,
                    "Invalid numeric prefix after '#': expected 'b', 'o' or 'x'.",
                    null);
        } else {
            cursor.advance(); // consume the prefix letter

            final int radix = radixFromPrefixLetter(prefixLetter);
            final String body = scanRadixDigits(radix);
            if (body.isEmpty()) {
                reportError(
                        ErrorCode.E0001,
                        start,
                        "Empty numeric literal after prefix '#" + prefixLetter + "'.",
                        null);
            } else {
                final String suffix = scanRadixSuffix();
                final String literal = "#" + prefixLetter + body + suffix;
                final Span span = Span.create(start, cursor.currentLocation());

                try {
                    tokens.add(Token.create(sourceId, parseRadixToken(radix, literal), span));
                } catch (final NumberFormatException e) {
                    reportRadixOverflow(radix, start, literal);
                }
            }
        }
    }

    private static char radixPrefixLetter(final int prefixChar) {
        return switch (prefixChar) {
            case 'b' -> 'b';
            case 'o' -> 'o';
            case 'x' -> 'x';
            default -> 0;
        };
    }

    private static int radixFromPrefixLetter(final char prefixLetter) {
        return switch (prefixLetter) {
            case 'b' -> Constants.RADIX_BINARY;
            case 'o' -> Constants.RADIX_OCTAL;
            default -> Constants.RADIX_HEX;
        };
    }

    private String scanRadixDigits(final int radix) {
        final StringBuilder body = new StringBuilder();
        int codePoint = cursor.peekCodePoint();
        while (isRadixDigit(codePoint, radix)) {
            body.appendCodePoint(codePoint);
            cursor.advance();
            codePoint = cursor.peekCodePoint();
        }
        return body.toString();
    }

    private String scanRadixSuffix() {
        String suffix = "";
        final int codePoint = cursor.peekCodePoint();
        if (codePoint == 'u' || codePoint == 'U') {
            cursor.advance();
            suffix = Character.toString(codePoint);
        }
        return suffix;
    }

    private static TokenKind parseRadixToken(final int radix, final String literal) {
        return switch (radix) {
            case Constants.RADIX_BINARY -> new TokenKind.Binary(BaseParsers.parseBinary(literal));
            case Constants.RADIX_OCTAL -> new TokenKind.Octal(BaseParsers.parseOctal(literal));
            default -> new TokenKind.Hexadecimal(BaseParsers.parseHex(literal));
        };
    }

    private void reportRadixOverflow(
            final int radix, final SourceLocation start, final String literal) {
        final ErrorCode code =
                switch (radix) {
                    case Constants.RADIX_BINARY -> ErrorCode.E0002;
                    case Constants.RADIX_OCTAL -> ErrorCode.E0003;
                    default -> ErrorCode.E0004;
                };
        reportError(code, start, "Numeric value out of range for literal '" + literal + "'.", null);
    }

    private static boolean isRadixDigit(final int codePoint, final int radix) {
        return switch (radix) {
            case Constants.RADIX_BINARY -> codePoint == '0' || codePoint == '1';
            case Constants.RADIX_OCTAL -> codePoint >= '0' && codePoint <= '7';
            case Constants.RADIX_HEX -> isHexDigit(codePoint);
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // String / char literals
    // ------------------------------------------------------------------

    /**
     * Scans a string literal delimited by double quotes, handling escape sequences and multi-byte
     * Unicode characters. If the string is not terminated before the end of the line or the end of
     * the source, {@link ErrorCode#E0005} is reported, but the token is still produced with the
     * content accumulated so far, to allow the parser to recover.
     */
    private void scanStringLiteral() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume the opening double quote

        final StringBuilder builder = new StringBuilder();
        boolean terminated = false;

        while (!cursor.isAtEnd()) {
            final int codePoint = cursor.peekCodePoint();
            if (codePoint == Constants.CHAR_DOUBLE_QUOTE) { // was: codePoint == '"'
                cursor.advance();
                terminated = true;
                break;
            }
            if (isLineTerminator(codePoint)) {
                break;
            }
            if (codePoint == Constants.CHAR_BACKSLASH) { // was: codePoint == '\\'
                consumeEscapeSequence(builder);
            } else {
                builder.appendCodePoint(codePoint);
                cursor.advance();
            }
        }

        if (!terminated) {
            reportError(ErrorCode.E0005, start, "Unterminated string literal.", null);
        }

        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, new TokenKind.StringLiteral(builder.toString()), span));
    }

    /**
     * Scans an ASCII/Unicode char literal, handling escape sequences (including {@code
     * \u005cu{XXXX}}). Reports {@link ErrorCode#E0006} if the literal is not correctly terminated
     * with a single character.
     */
    private void scanCharLiteral() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume the opening quote

        final StringBuilder builder = new StringBuilder();

        if (cursor.isAtEnd() || isLineTerminator(cursor.peekCodePoint())) {
            reportError(ErrorCode.E0006, start, "Unterminated char literal.", null);
        } else if (cursor.peekCodePoint() == Constants.CHAR_SINGLE_QUOTE) {
            cursor.advance();
            reportError(ErrorCode.E0006, start, "Empty char literal.", null);
        } else {
            consumeCharLiteralBody(builder, start);
        }

        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, new TokenKind.CharLiteral(builder.toString()), span));
    }

    /**
     * Consumes the body of a char literal (after the opening quote has been consumed), including
     * the closing quote. Reports an error if the literal contains more than one character or is not
     * properly terminated.
     */
    private void consumeCharLiteralBody(final StringBuilder builder, final SourceLocation start) {
        consumeSingleCharOrEscape(builder);
        if (isClosingQuote()) {
            cursor.advance();
        } else {
            skipToClosingQuoteOrLineEnd();
            reportError(
                    ErrorCode.E0006,
                    start,
                    "Unterminated char literal or containing more than one character.",
                    null);
        }
    }

    /** Consumes exactly one logical character or escape sequence into {@code builder}. */
    private void consumeSingleCharOrEscape(final StringBuilder builder) {
        if (cursor.peekCodePoint() == Constants.CHAR_BACKSLASH) {
            consumeEscapeSequence(builder);
        } else {
            builder.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }
    }

    /**
     * Returns {@code true} if the cursor is not at end and the current code point is a single-quote
     * closing delimiter.
     */
    private boolean isClosingQuote() {
        return !cursor.isAtEnd() && cursor.peekCodePoint() == Constants.CHAR_SINGLE_QUOTE;
    }

    /**
     * Advances past any extra characters until the closing single-quote or a line terminator is
     * reached, then consumes the closing quote if present.
     */
    private void skipToClosingQuoteOrLineEnd() {
        while (!cursor.isAtEnd()
                && cursor.peekCodePoint() != Constants.CHAR_SINGLE_QUOTE
                && !isLineTerminator(cursor.peekCodePoint())) {
            cursor.advance();
        }
        if (isClosingQuote()) {
            cursor.advance();
        }
    }

    /**
     * Consumes and resolves an escape sequence starting with {@code '\'} under the cursor, adding
     * the resulting code point(s) to {@code builder}. If the sequence is unrecognized or malformed,
     * {@link ErrorCode#E0007} is reported.
     */
    private void consumeEscapeSequence(final StringBuilder builder) {
        final SourceLocation escapeStart = cursor.currentLocation();
        cursor.advance(); // consume '\'

        if (cursor.isAtEnd()) {
            reportError(ErrorCode.E0007, escapeStart, "Unterminated escape sequence.", null);
            return;
        }

        final int codePoint = cursor.peekCodePoint();
        switch (codePoint) {
            case 'n' -> {
                builder.append('\n');
                cursor.advance();
            }
            case 'r' -> {
                builder.append('\r');
                cursor.advance();
            }
            case 't' -> {
                builder.append('\t');
                cursor.advance();
            }
            case '\\' -> {
                builder.append('\\');
                cursor.advance();
            }
            case '\'' -> {
                builder.append('\'');
                cursor.advance();
            }
            case '"' -> {
                builder.append('"');
                cursor.advance();
            }
            case '0' -> {
                builder.append('\0');
                cursor.advance();
            }
            case 'x' -> consumeHexEscape(builder, escapeStart);
            case 'u' -> consumeUnicodeEscape(builder, escapeStart, 'u');
            case 'U' -> consumeUnicodeEscape(builder, escapeStart, 'U');
            default -> {
                reportError(
                        ErrorCode.E0007,
                        escapeStart,
                        "Invalid escape sequence: '\\" + Character.toString(codePoint) + "'.",
                        null);
                // recovery: consume the character anyway to avoid infinite loops.
                builder.appendCodePoint(codePoint);
                cursor.advance();
            }
        }
    }

    /**
     * Consumes a Unicode escape sequence in the format {@code \u005cu{XXXX}}, where {@code XXXX} is
     * a sequence of hexadecimal digits representing a valid Unicode code point. At the time of the
     * call, the cursor must be positioned on the 'u' following the backslash.
     */
    private void consumeUnicodeEscape(
            final StringBuilder builder,
            final SourceLocation escapeStart,
            final char prefixLetter) {

        final int maxDigits =
                (prefixLetter == 'U')
                        ? Constants.UNICODE_ESCAPE_LONG_DIGIT_COUNT // 8
                        : Constants.UNICODE_ESCAPE_SHORT_DIGIT_COUNT; // 4

        cursor.advance(); // consume 'u' or 'U'

        boolean valid = expectOpenBrace(escapeStart);

        String hex = null;
        if (valid) {
            hex = consumeHexDigits(maxDigits);

            // Check if the digit limit was reached before the '}'
            if (!cursor.isAtEnd()
                    && cursor.peekCodePoint() != '}'
                    && isHexDigit(cursor.peekCodePoint())) {
                reportError(
                        ErrorCode.E0007,
                        escapeStart,
                        "Unicode escape sequence with too many digits: "
                                + "maximum "
                                + maxDigits
                                + " hexadecimal digits for '\\"
                                + prefixLetter
                                + "'.",
                        null);
                drainExcessHexDigits();
                valid = false;
            } else {
                valid = expectClosingBrace(escapeStart, hex);
            }
        }

        if (valid) {
            appendUnicodeCodePoint(builder, escapeStart, hex);
        }
    }

    /**
     * Recovery method: advances the cursor past excess hexadecimal digits until reaching {@code
     * '}'}, a non-hexadecimal character, or the end of the source. If a {@code '}'} is present at
     * the end, it is consumed to keep the cursor in a consistent state and allow lexing to
     * continue.
     */
    private void drainExcessHexDigits() {
        while (!cursor.isAtEnd()
                && cursor.peekCodePoint() != '}'
                && isHexDigit(cursor.peekCodePoint())) {
            cursor.advance();
        }
        // Consume the closing '}' if present, to avoid leaving the cursor on it
        if (!cursor.isAtEnd() && cursor.peekCodePoint() == '}') {
            cursor.advance();
        }
    }

    private boolean expectOpenBrace(final SourceLocation escapeStart) {
        boolean isValid = true;

        if (cursor.isAtEnd() || cursor.peekCodePoint() != '{') {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Invalid unicode escape sequence: expected '{' after \\u.",
                    null);
            isValid = false;
        } else {
            cursor.advance(); // consume '{'
        }

        return isValid;
    }

    private String consumeHexDigits(final int maxDigits) {
        final StringBuilder hex = new StringBuilder();
        while (hex.length() < maxDigits
                && !cursor.isAtEnd()
                && cursor.peekCodePoint() != '}'
                && isHexDigit(cursor.peekCodePoint())) {
            hex.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }
        return hex.toString();
    }

    private boolean expectClosingBrace(final SourceLocation escapeStart, final String hex) {
        boolean result = true;

        if (hex.isEmpty()) {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Unicode escape sequence without hexadecimal digits.",
                    null);
            result = false;
        } else if (cursor.isAtEnd() || cursor.peekCodePoint() != '}') {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Unterminated unicode escape sequence: missing '}'.",
                    null);
            result = false;
        } else {
            cursor.advance(); // consume '}'
        }

        return result;
    }

    private void appendUnicodeCodePoint(
            final StringBuilder builder, final SourceLocation escapeStart, final String hex) {
        try {
            final int codePointValue = Integer.parseInt(hex, 16);
            if (Character.isValidCodePoint(codePointValue)) {
                builder.appendCodePoint(codePointValue);
            } else {
                reportError(
                        ErrorCode.E0007, escapeStart, "Invalid unicode code point: U+" + hex, null);
            }
        } catch (final NumberFormatException e) {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Invalid hexadecimal value in unicode escape sequence.",
                    null);
        }
    }

    /**
     * Consumes a two-digit hexadecimal escape sequence in the format {@code \xHH}, where {@code HH}
     * represents a byte value between {@code 0x00} and {@code 0xFF}. At the time of the call, the
     * cursor must be positioned on the 'x' following the backslash. If the number of digits differs
     * from {@link Constants#HEX_ESCAPE_DIGIT_COUNT}, {@link ErrorCode#E0007} is reported.
     */
    private void consumeHexEscape(final StringBuilder builder, final SourceLocation escapeStart) {
        cursor.advance(); // consume 'x'

        final StringBuilder hex = new StringBuilder();
        while (hex.length() < Constants.HEX_ESCAPE_DIGIT_COUNT
                && !cursor.isAtEnd()
                && isHexDigit(cursor.peekCodePoint())) {
            hex.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }

        if (hex.length() != Constants.HEX_ESCAPE_DIGIT_COUNT) {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Invalid hexadecimal escape sequence: expected exactly "
                            + Constants.HEX_ESCAPE_DIGIT_COUNT
                            + " hexadecimal digits after \\x.",
                    null);
            return;
        }

        final int value = Integer.parseInt(hex.toString(), 16);
        builder.appendCodePoint(value);
    }

    private static boolean isLineTerminator(final int codePoint) {
        return codePoint == Constants.LINE_FEED
                || codePoint == Constants.CARRIAGE_RETURN
                || codePoint == Constants.LINE_SEPARATOR
                || codePoint == Constants.PARAGRAPH_SEPARATOR;
    }

    private static boolean isHexDigit(final int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'a' && codePoint <= 'f')
                || (codePoint >= 'A' && codePoint <= 'F');
    }

    private void reportError(
            final ErrorCode errorCode,
            final SourceLocation start,
            final String message,
            final String help) {
        final Span span = Span.create(start, cursor.currentLocation());
        errors.add(CompileError.lexerError(errorCode, message, span, help));
    }

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.lineCount();
    }
}

package org.dersbian.compiler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Holds shared constant values used throughout the compiler, including character codes and UTF-8
 * encoding boundaries/lengths.
 *
 * <p>This class is not meant to be instantiated.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"PMD.LongVariable", "PMD.DataClass"})
public final class Constants {
    /** Code point for the line feed character ({@code \n}). */
    public static final int LINE_FEED = '\n';

    /** Code point for the carriage return character ({@code \r}). */
    public static final int CARRIAGE_RETURN = '\r';

    /** Code point for the Unicode line separator character ({@code \u2028}). */
    public static final int LINE_SEPARATOR = '\u2028';

    /** Code point for the Unicode paragraph separator character ({@code \u2029}). */
    public static final int PARAGRAPH_SEPARATOR = '\u2029';

    /** Unicode byte-order mark, occasionally left at the start of UTF-8 encoded files. */
    public static final int BYTE_ORDER_MARK = '\uFEFF';

    /** Exclusive upper bound (code point) for a 1-byte UTF-8 encoding. */
    public static final int UTF8_ONE_BYTE_LIMIT = 0x80;

    /** Exclusive upper bound (code point) for a 2-byte UTF-8 encoding. */
    public static final int UTF8_TWO_BYTE_LIMIT = 0x800;

    /** Exclusive upper bound (code point) for a 3-byte UTF-8 encoding. */
    public static final int UTF8_THREE_BYTE_LIMIT = 0x10000;

    /** Number of bytes used by a 1-byte UTF-8 encoding. */
    public static final int UTF8_ONE_BYTE_LENGTH = 1;

    /** Number of bytes used by a 2-byte UTF-8 encoding. */
    public static final int UTF8_TWO_BYTE_LENGTH = 2;

    /** Number of bytes used by a 3-byte UTF-8 encoding. */
    public static final int UTF8_THREE_BYTE_LENGTH = 3;

    /** Number of bytes used by a 4-byte UTF-8 encoding. */
    public static final int UTF8_FOUR_BYTE_LENGTH = 4;

    /** Boolean literal "true". */
    public static final String TRUE_LITERAL = "true";

    /** Boolean literal "false". */
    public static final String FALSE_LITERAL = "false";

    /** Radix used for {@code #b} binary literals. */
    public static final int RADIX_BINARY = 2;

    /** Radix used for {@code #o} octal literals. */
    public static final int RADIX_OCTAL = 8;

    /** Radix used for {@code #x} hexadecimal literals. */
    public static final int RADIX_HEX = 16;

    /** Fixed number of hex digits required by a {@code \xHH} escape sequence. */
    public static final int HEX_ESCAPE_DIGIT_COUNT = 2;

    /** Character constant for double quote ("). */
    public static final int CHAR_DOUBLE_QUOTE = '"';

    /** Character constant for single quote ('). */
    public static final int CHAR_SINGLE_QUOTE = '\'';

    /** Character constant for backslash (\). */
    public static final int CHAR_BACKSLASH = '\\';

    /** Maximum hex digit count for a \u005cu Unicode escape (e.g. \u005cu{004F}). */
    public static final int UNICODE_ESCAPE_SHORT_DIGIT_COUNT = 4;

    /** Maximum hex digit count for a \U Unicode escape (e.g. \U{0010FFFF}). */
    public static final int UNICODE_ESCAPE_LONG_DIGIT_COUNT = 8;

    /** Character constant for asterisk (*). */
    public static final char CHAR_ASTERISK = '*';

    /** Rough character-per-token estimate used to presize the token list and avoid resizing. */
    public static final int ESTIMATED_CHARS_PER_TOKEN = 5;
}


```

---
**Title**
Implementation Flow Review for Exercise

**Role & stance**
You are a technical analyst tasked with objectively evaluating implementation plans.

**Task**
Generate the implementation flow for the given exercise and highlight any potential problems and suggested improvements.

**Context**
Details of the exercise (lexer) are provided separately.

**Inputs available**
- Exercise description (placeholder: [FILL: exercise details])

**Output requirements**
- A step‑by‑step implementation flow.
- A list of identified issues.
- A list of recommended improvements.
- Use clear headings and bullet points.

**Constraints / Do-nots**
- Do not alter the original exercise details.
- Do not add speculative features not mentioned in the exercise.
- Preserve the original terminology (e.g., “exer”).

**Examples / References**
*No examples provided.*

**Execution checklist**
- [ ] Implementation flow is complete and ordered.
- [ ] All potential problems are listed.
- [ ] All improvement suggestions are included.
- [ ] Output follows the required structure.

**Conflict resolution**
If any instruction conflicts, prioritize “Constraints / Do-nots” over “Output requirements”.

the lexer is writen in java 25