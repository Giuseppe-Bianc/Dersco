package org.dersbian.compiler.syntax.ast;

import java.util.Objects;

/** Configuration for parent/current/child branch types. */
public record BranchConfig(BranchType parentType, BranchType currentType, BranchType childType) {
    /**
     * Compact constructor that validates that none of the branch types are {@code null}.
     *
     * @param parentType the branch type of the parent node
     * @param currentType the branch type of the current node
     * @param childType the branch type of the child node
     * @throws NullPointerException if any of the branch types are {@code null}
     */
    public BranchConfig {
        Objects.requireNonNull(parentType, "parentType must not be null");
        Objects.requireNonNull(currentType, "currentType must not be null");
        Objects.requireNonNull(childType, "childType must not be null");
    }
}
