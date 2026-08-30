package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests the immutable structural invariants of scope snapshots. */
class ScopeContractTest {
    @Test
    void globalScopeRejectsParentDepthAndOwner() {
        final ScopeId id = new ScopeId(1);

        assertThatThrownBy(
                        () -> new Scope(id, ScopeKind.GLOBAL, Optional.of(id), 0, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new Scope(id, ScopeKind.GLOBAL, Optional.empty(), 1, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new Scope(id, ScopeKind.GLOBAL, Optional.empty(), 0, Optional.of(new SymbolId(1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void functionScopeRequiresOwnerAndOtherScopesRejectOwners() {
        final ScopeId parent = new ScopeId(1);
        final SymbolId owner = new SymbolId(1);

        assertThatThrownBy(
                        () -> new Scope(
                                new ScopeId(2),
                                ScopeKind.FUNCTION,
                                Optional.of(parent),
                                1,
                                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new Scope(
                                new ScopeId(2),
                                ScopeKind.BLOCK,
                                Optional.of(parent),
                                1,
                                Optional.of(owner)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonGlobalScopeRequiresParentAndPositiveDepth() {
        final ScopeId parent = new ScopeId(1);

        assertThatThrownBy(
                        () -> new Scope(
                                new ScopeId(2),
                                ScopeKind.BLOCK,
                                Optional.empty(),
                                1,
                                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new Scope(
                                new ScopeId(2),
                                ScopeKind.BLOCK,
                                Optional.of(parent),
                                0,
                                Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
