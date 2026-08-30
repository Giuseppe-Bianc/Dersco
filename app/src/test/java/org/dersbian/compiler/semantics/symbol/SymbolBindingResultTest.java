package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the immutable result model used by AST binding consumers. */
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AtLeastOneConstructor"})
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
        assertThatThrownBy(() -> new SymbolBindingResult(Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("declarations must not contain null");
    }
}
