package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
@Getter
public class Lexer {
    /** Tracker used to map source positions to line numbers. */
    private final LineTracker lineTracker;

    /** Collected tokens produced by the lexer. */
    private final List<Token> tokens = new ArrayList<>();

    /** Collected lexical and syntax errors. */
    private final List<CompileError> errors = new ArrayList<>();

    /** Stable identifier of the source being lexed. */
    private final SourceId sourceId;

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        this.lineTracker = LineTracker.fromText(source);
    }

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.getLines().size() + 1;
    }
    // TODO: wire up the real lexer.
}
