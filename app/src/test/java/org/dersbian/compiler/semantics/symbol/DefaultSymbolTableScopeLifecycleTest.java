package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies structured scope lifecycle semantics independently from declaration tests. */
@SuppressWarnings({
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.AtLeastOneConstructor",
    "PMD.CloseResource"
})
class DefaultSymbolTableScopeLifecycleTest {
    @Test
    void openScopeReturnsStableSnapshotAndCloseIsIdempotent() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        final ScopeHandle handle = table.openScope(ScopeKind.BLOCK);
        final Scope opened = handle.scope();

        assertThat(opened).isEqualTo(table.currentScope());
        assertThat(opened.parentId()).contains(table.globalScope().id());
        assertThat(opened.depth()).isEqualTo(1);

        handle.close();
        handle.close();

        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        table.assertConsistent();
    }

    @Test
    void closingAnOuterHandleBeforeItsNestedScopeIsRejected() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        final ScopeHandle outer = table.openScope(ScopeKind.BLOCK);
        table.enterScope(ScopeKind.LOOP);

        assertThatThrownBy(outer::close).isInstanceOf(IllegalStateException.class);
        assertThat(table.currentScope().kind()).isEqualTo(ScopeKind.LOOP);
        table.assertConsistent();

        table.exitScope();
        outer.close();
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        table.assertConsistent();
    }

    @Test
    void nestedHandlesMustCloseInLifoOrder() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        final ScopeHandle outer = table.openScope(ScopeKind.BLOCK);
        final ScopeHandle inner = table.openScope(ScopeKind.BLOCK);

        inner.close();
        assertThat(table.currentScope()).isEqualTo(outer.scope());

        outer.close();
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        table.assertConsistent();
    }

    @Test
    void closingAnHandleDoesNotRemoveItsHistoricalScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        final ScopeHandle handle = table.openScope(ScopeKind.BLOCK);
        final ScopeId scopeId = handle.scope().id();
        handle.close();

        assertThat(table.findScope(scopeId)).contains(handle.scope());
        assertThat(table.lookupLocal(scopeId, "missing")).isEmpty();
        table.assertConsistent();
    }
}
