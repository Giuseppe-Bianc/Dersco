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
    /** Tracker used to map source positions to line numbers. */
    @Getter private final LineTracker lineTracker;

    /** Collected tokens produced by the lexer. */
    private final List<Token> tokens = new ArrayList<>();

    /** Collected lexical and syntax errors. */
    private final List<CompileError> errors = new ArrayList<>();

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
        final SourceLocation startLocation = getCurrentLocation();
        position++;
        columnNumber++;
        lineNumber++;
        final Span tokenPosition = new Span(startLocation, getCurrentLocation());
        final Token token = new Token(sourceId, TokenKind.Simple.EOF, tokenPosition);
        tokens.add(token);
        return new LexerResult(tokens, errors);
    }

    private SourceLocation getCurrentLocation() {
        return new SourceLocation(lineNumber, columnNumber, position, position, position, position);
    }

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        this.lineTracker = LineTracker.fromText(source);
        this.position = 0;
        this.columnNumber = 1;
        this.lineNumber = 1;
    }

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.lineCount();
    }
    // TODO: wire up the real  lexer.
}
