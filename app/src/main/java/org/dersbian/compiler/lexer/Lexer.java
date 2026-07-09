package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
public class Lexer {

    /** Code point for the line feed character ({@code \n}). */
    private static final int LINE_FEED = '\n';

    /** Code point for the carriage return character ({@code \r}). */
    private static final int CARRIAGE_RETURN = '\r';

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

    /** Current absolute offset into the source text. */
    private int position;

    /** Current 1-based line number in the source text. */
    private int lineNumber;

    /** Current 1-based column number in the source text. */
    private int columnNumber;

    /**
     * Tokenizes the source text and returns the resulting tokens and any errors encountered.
     *
     * @return the result containing the produced tokens and collected errors.
     */
    public LexerResult tokenize() {
        while (!isAtEnd()) {
            // TODO: wire up the real lexer. Codepoints (not raw chars) are consumed one
            // at a time so that surrogate pairs (characters outside the BMP, e.g. many
            // emoji) are never split in half.
            advance();
        }
        final SourceLocation eofLocation = getCurrentLocation();
        final Span tokenPosition = Span.point(eofLocation);
        final Token token = new Token(sourceId, TokenKind.Simple.EOF, tokenPosition);
        tokens.add(token);
        return new LexerResult(tokens, errors);
    }

    private SourceLocation getCurrentLocation() {
        return SourceLocation.create(lineNumber, columnNumber, position);
    }

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        this.source = source;
        this.length = source.length();
        this.lineTracker = LineTracker.fromText(source);
        this.position = 0;
        this.columnNumber = 1;
        this.lineNumber = 1;
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
     * surrogate pairs, and updates line/column tracking. Line terminators {@code \n}, {@code \r}
     * and {@code \r\n} are all normalized to a single line break.
     *
     * @return the consumed code point, or {@code -1} at end of source.
     */
    private int advance() {
        final int codePoint = peekCodePoint();
        if (codePoint != -1) {
            position += Character.charCount(codePoint);
            switch (codePoint) {
                case LINE_FEED -> {
                    lineNumber++;
                    columnNumber = 1;
                }
                case CARRIAGE_RETURN -> {
                    if (peekCodePoint() != LINE_FEED) {
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
    // TODO: wire up the real  lexer.
}
