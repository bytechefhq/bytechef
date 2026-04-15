/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.domain;

import java.util.Objects;
import java.util.Set;

/**
 * Scope hierarchy for a connection's visibility.
 *
 * <p>
 * <b>Ordinal stability:</b> values are persisted as INT ordinals (see Liquibase migrations). Do not reorder or insert
 * existing values — append only. The explicit {@code rank} field drives {@link #isAtLeast(ConnectionVisibility)} AND
 * doubles as the stable persistence code (see {@link #getCode()}): reordering the enum constants would corrupt every
 * persisted row on upgrade. Hierarchy: PRIVATE &lt; PROJECT &lt; WORKSPACE &lt; ORGANIZATION.
 *
 * <p>
 * {@code ConnectionVisibilityTest#testConnectionVisibilityOrdinalsAreStableForJdbcPersistence} pins the current
 * rank/ordinal mapping — a reorder fails loud at build time. When a future change introduces a custom Spring Data JDBC
 * {@code Converter} pair it should read/write {@link #getCode()} rather than {@link #ordinal()} so the mapping becomes
 * fully code-based and the test can be replaced by a converter-round-trip test.
 *
 * <p>
 * <b>State machine.</b> {@link #canTransitionTo(ConnectionVisibility)} centralizes the legal transitions so callers do
 * not re-invent them at each mutation site. ORGANIZATION is reachable only through the organization-scoped facade (not
 * from workspace-level promote/demote) and is therefore absent from every workspace transition set. Same-state
 * transitions are rejected — callers that want an idempotent no-op must short-circuit before invoking the transition.
 *
 * @author Ivica Cardic
 */
public enum ConnectionVisibility {

    PRIVATE(0), // Visible only to creator
    PROJECT(1), // Visible to members of shared projects
    WORKSPACE(2), // Visible to all workspace members
    ORGANIZATION(3); // Visible to all members across all workspaces

    // PRIVATE → ORGANIZATION is the initial-tag path from OrganizationConnectionFacade (a connection
    // is created as PRIVATE by ConnectionFacadeImpl.create, then promoted in the same transaction).
    // ORGANIZATION is terminal otherwise — org connections are deleted rather than demoted, so no
    // outbound transitions are legal from that state.
    private static final Set<ConnectionVisibility> PRIVATE_TRANSITIONS = Set.of(PROJECT, WORKSPACE, ORGANIZATION);
    private static final Set<ConnectionVisibility> PROJECT_TRANSITIONS = Set.of(PRIVATE, WORKSPACE);
    private static final Set<ConnectionVisibility> WORKSPACE_TRANSITIONS = Set.of(PRIVATE, PROJECT);
    private static final Set<ConnectionVisibility> ORGANIZATION_TRANSITIONS = Set.of();

    private final int rank;

    ConnectionVisibility(int rank) {
        this.rank = rank;
    }

    /**
     * Returns the stable integer code for JDBC persistence. Semantically identical to {@link #ordinal()} today but
     * decouples the wire representation from the enum declaration order — a future reorder under code-based persistence
     * would not corrupt persisted rows.
     */
    public int getCode() {
        return rank;
    }

    /**
     * Inverse of {@link #getCode()}: resolves an integer code back to the enum constant. Throws
     * {@link IllegalArgumentException} for unknown codes rather than returning null — this is the contract a future
     * Spring Data JDBC {@code ReadingConverter} should use so a corrupted INT surfaces loudly.
     */
    public static ConnectionVisibility fromCode(int code) {
        for (ConnectionVisibility visibility : values()) {
            if (visibility.rank == code) {
                return visibility;
            }
        }

        throw new IllegalArgumentException("Unknown ConnectionVisibility code: " + code);
    }

    /**
     * Returns true if this visibility level is at least as broad as the given level. Hierarchy: PRIVATE &lt; PROJECT
     * &lt; WORKSPACE &lt; ORGANIZATION.
     */
    public boolean isAtLeast(ConnectionVisibility other) {
        Objects.requireNonNull(other, "other");

        return this.rank >= other.rank;
    }

    /**
     * Returns true if a connection currently at {@code this} visibility may transition to {@code target}. Same-state
     * transitions return false — callers that want an idempotent no-op should check for equality first.
     */
    public boolean canTransitionTo(ConnectionVisibility target) {
        Objects.requireNonNull(target, "target");

        return switch (this) {
            case PRIVATE -> PRIVATE_TRANSITIONS.contains(target);
            case PROJECT -> PROJECT_TRANSITIONS.contains(target);
            case WORKSPACE -> WORKSPACE_TRANSITIONS.contains(target);
            case ORGANIZATION -> ORGANIZATION_TRANSITIONS.contains(target);
        };
    }
}
