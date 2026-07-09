package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
public class Lexer {

    /** Tracker used to map source positions to line numbers. */
    @Getter private final LineTracker lineTracker;

    /** Collected tokens produced by the lexer. */
    private final List<Token> tokens = new ArrayList<>();

    /** Collected lexical and syntax errors. */
    private final List<CompileError> errors = new ArrayList<>();

    /** Source text being lexed (decoded to a Java {@code String}, i.e. UTF-16 internally). */
    private final String source;

    /** Length of the source text expressed in UTF-16 code units. */
    private final int length;

    /** Stable identifier of the source being lexed. */
    private final SourceId sourceId;

    /**
     * Current absolute offset into the source text, expressed in UTF-8 bytes. This mirrors the
     * on-disk byte offset of the original UTF-8 encoded file and is surfaced via {@link
     * SourceLocation#utf8Offset()}, unlike {@link #position} which counts UTF-16 code units.
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
    private int lineNumber;

    /** Current 1-based column number in the source text. */
    private int columnNumber;

    /**
     * Removes a leading UTF-8 byte-order mark ({@code U+FEFF}), if present. Some editors write it
     * at the start of UTF-8 encoded files; if left untouched it would surface as a spurious invalid
     * character at position zero.
     */
    private static String stripByteOrderMark(final String text) {
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
     * lets {@link #utf8Offset} track the real byte offset in the original source file, even though
     * the in-memory representation is a Java {@code String} (UTF-16).
     */
    private static int utf8ByteLength(final int codePoint) {
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

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        this.source = stripByteOrderMark(source);
        this.length = this.source.length();
        this.lineTracker = LineTracker.fromText(this.source);
        this.utf8Offset = 0L;
        this.position = 0;
        this.codePointOffset = 0L;
        this.columnNumber = 1;
        this.lineNumber = 1;
    }

    /**
     * Tokenizes the source text and returns the resulting tokens and any errors encountered.
     *
     * @return the result containing the produced tokens and collected errors.
     */
    public LexerResult tokenize() {
        while (!isAtEnd()) {
            // TODO: wire up the real lexer. Codepoints (not raw chars) are consumed one
            // at a time so that surrogate pairs (characters outside the BMP, e.g. many
            // emoji) are never split in half, and UTF-8 byte / code point offsets stay in sync.
            advance();
        }
        final SourceLocation eofLocation = getCurrentLocation();
        final Span tokenPosition = Span.point(eofLocation);
        final Token token = new Token(sourceId, TokenKind.Simple.EOF, tokenPosition);
        tokens.add(token);
        return new LexerResult(tokens, errors);
    }

    /**
     * Builds the {@link SourceLocation} for the current cursor position, fully populating the
     * UTF-16 based {@code offset}/{@code index}, the UTF-8 byte offset and the code point offset.
     */
    private SourceLocation getCurrentLocation() {
        return SourceLocation.create(
                lineNumber, columnNumber, position, position, utf8Offset, codePointOffset);
    }

    /** Returns {@code true} if the lexer has consumed the entire source text. */
    private boolean isAtEnd() {
        return position >= length;
    }

    /**
     * Returns the Unicode code point at the current position without consuming it, or {@code -1} if
     * the end of the source has been reached.
     */
    private int peekCodePoint() {
        return isAtEnd() ? -1 : source.codePointAt(position);
    }

    /**
     * Consumes and returns the Unicode code point at the current position, correctly advancing past
     * surrogate pairs, and updates line/column tracking along with the UTF-8 byte offset and code
     * point count. Line terminators {@code \n}, {@code \r} and {@code \r\n} are all normalized to a
     * single line break.
     *
     * @return the consumed code point, or {@code -1} at end of source.
     */
    private int advance() {
        final int codePoint = peekCodePoint();
        if (codePoint != -1) {
            position += Character.charCount(codePoint);
            utf8Offset += utf8ByteLength(codePoint);
            codePointOffset++;
            switch (codePoint) {
                case Constants.LINE_FEED -> {
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

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.lineCount();
    }
    // TODO: wire up the real lexer.
}
