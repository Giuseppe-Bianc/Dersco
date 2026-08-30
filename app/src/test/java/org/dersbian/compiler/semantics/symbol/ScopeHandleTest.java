package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests structured scope lifecycle and strict LIFO handling. */
class ScopeHandleTest {
    @Test
    void closesTheScopeItOpenedAndRestoresParent() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope global = table.currentScope();

        final ScopeHandle handle = table.openScope(ScopeKind.BLOCK);
        assertThat(table.currentScope()).isEqualTo(handle.scope());

        handle.close();

        assertThat(table.currentScope()).isEqualTo(global);
        assertThat(table.findScope(handle.scope().id())).contains(handle.scope());
    }

    @Test
    void rejectsDoubleClose() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final ScopeHandle handle = table.openScope(ScopeKind.BLOCK);

        handle.close();

        assertThatThrownBy(handle::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    void rejectsClosingAnOlderHandleWhileAChildIsActive() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final ScopeHandle outer = table.openScope(ScopeKind.BLOCK);
        table.openScope(ScopeKind.BLOCK);

        assertThatThrownBy(outer::close)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not the active scope");
    }

    @Test
    void tryWithResourcesRestoresParentAfterExceptionalExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope global = table.currentScope();

        assertThatThrownBy(
                        () -> {
                            try (ScopeHandle ignored = table.openScope(ScopeKind.LOOP)) {
                                throw new IllegalStateException("synthetic failure");
                            }
                        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic failure");

        assertThat(table.currentScope()).isEqualTo(global);
    }
}
