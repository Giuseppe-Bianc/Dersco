package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the immutable result model used by AST binding consumers. */
class SymbolBindingResultTest {
    @Test
    void declarationResultsAreDefensivelyCopied() {
        final List<DeclarationResult> declarations = new ArrayList<>();
        final SymbolBindingResult result = new SymbolBindingResult(declarations);

        declarations.clear();

        assertThat(result.declarations()).isEmpty();
        assertThatThrownBy(() -> result.declarations().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void nullDeclarationResultIsRejected() {
        assertThatThrownBy(() -> new SymbolBindingResult(List.of((DeclarationResult) null)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("declarations must not contain null");
    }
}
