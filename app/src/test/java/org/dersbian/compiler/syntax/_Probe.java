package org.dersbian.compiler.syntax;

import java.nio.file.Files;
import java.nio.file.Path;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.junit.jupiter.api.Test;

class _Probe {

    @Test
    void probeBreakContinue() throws Exception {
        final Path p = Path.of("dr_files/break_continue_loops.dr");
        final Path resolved = Files.exists(p) ? p : Path.of("..", p.toString());
        final String s = Files.readString(resolved);
        final Lexer l = new Lexer(resolved, s);
        final LexerResult lr = l.tokenize();
        final Parser pa = new Parser(lr.tokens(), resolved);
        final ParseResult pr = pa.parse();
        System.out.println("[PROBE] errors=" + pr.errors().size());
        pr.errors().forEach(e -> System.out.println("[PROBE] err: " + e.errorMessage()));
    }
}
