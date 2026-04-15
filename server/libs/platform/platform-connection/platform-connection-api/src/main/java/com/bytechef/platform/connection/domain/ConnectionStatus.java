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

import java.util.Set;

/**
 * Connection lifecycle state machine.
 *
 * <p>
 * Transitions: ACTIVE -> PENDING_REASSIGNMENT -> ACTIVE (on reassignment) or REVOKED. ACTIVE -> REVOKED.
 *
 * <p>
 * <b>REVOKED is terminal by design.</b> A revocation implies the underlying credential has been (or must be treated as)
 * compromised — reviving that row back to ACTIVE would reuse the same id/audit trail for what is semantically a new
 * grant. Recovery is intentionally forced through "create a new connection" so the audit chain remains monotonic and
 * operators cannot accidentally reintroduce a withdrawn credential.
 *
 * <p>
 * <b>Ordinal stability:</b> values are persisted as INT ordinals (see Liquibase migrations). Do not reorder or insert
 * existing values — append only. Use {@link #getCode()} / {@link #fromCode(int)} when wiring a future Spring Data JDBC
 * {@code Converter} pair so the persistence mapping becomes code-based rather than ordinal-based.
 *
 * @author Ivica Cardic
 */
public enum ConnectionStatus {

    ACTIVE(0),
    PENDING_REASSIGNMENT(1),
    REVOKED(2);

    private static final Set<ConnectionStatus> ACTIVE_TRANSITIONS = Set.of(PENDING_REASSIGNMENT, REVOKED);
    private static final Set<ConnectionStatus> PENDING_REASSIGNMENT_TRANSITIONS = Set.of(ACTIVE, REVOKED);
    private static final Set<ConnectionStatus> REVOKED_TRANSITIONS = Set.of();

    private final int code;

    ConnectionStatus(int code) {
        this.code = code;
    }

    /**
     * Returns the stable integer code for JDBC persistence. Semantically identical to {@link #ordinal()} today but
     * decouples the wire representation from the enum declaration order — a future reorder under code-based persistence
     * would not corrupt persisted rows.
     */
    public int getCode() {
        return code;
    }

    /**
     * Inverse of {@link #getCode()}: resolves an integer code back to the enum constant. Throws
     * {@link IllegalArgumentException} for unknown codes rather than returning null so a corrupted INT surfaces loudly.
     */
    public static ConnectionStatus fromCode(int code) {
        for (ConnectionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown ConnectionStatus code: " + code);
    }

    /**
     * Returns true if a connection currently at {@code this} status may transition to {@code target}. Same-state
     * transitions return false — callers that want an idempotent no-op should check for equality first, matching the
     * convention established by {@link ConnectionVisibility#canTransitionTo(ConnectionVisibility)}. The reassignment
     * facade relies on this method to classify rows as skipped-vs-failed.
     */
    public boolean canTransitionTo(ConnectionStatus target) {
        return switch (this) {
            case ACTIVE -> ACTIVE_TRANSITIONS.contains(target);
            case PENDING_REASSIGNMENT -> PENDING_REASSIGNMENT_TRANSITIONS.contains(target);
            case REVOKED -> REVOKED_TRANSITIONS.contains(target);
        };
    }
}
