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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionStatusTest {

    @Test
    void testActiveCanTransitionToPendingReassignment() {
        assertThat(ConnectionStatus.ACTIVE.canTransitionTo(ConnectionStatus.PENDING_REASSIGNMENT)).isTrue();
    }

    @Test
    void testActiveCanTransitionToRevoked() {
        assertThat(ConnectionStatus.ACTIVE.canTransitionTo(ConnectionStatus.REVOKED)).isTrue();
    }

    @Test
    void testActiveCannotTransitionToActive() {
        assertThat(ConnectionStatus.ACTIVE.canTransitionTo(ConnectionStatus.ACTIVE)).isFalse();
    }

    /**
     * {@link ConnectionStatus} is persisted as an INT ordinal on {@code connection.status}. Reordering or inserting a
     * new value anywhere other than the end would silently corrupt every row on upgrade. This test pins the ordinals so
     * any accidental reorder fails loud at build time — new values MUST be appended at the end of the enum declaration.
     */
    @Test
    void testConnectionStatusOrdinalsAreStableForJdbcPersistence() {
        assertThat(ConnectionStatus.ACTIVE.ordinal()).isEqualTo(0);
        assertThat(ConnectionStatus.PENDING_REASSIGNMENT.ordinal()).isEqualTo(1);
        assertThat(ConnectionStatus.REVOKED.ordinal()).isEqualTo(2);
        assertThat(ConnectionStatus.values()).hasSize(3);
    }

    @Test
    void testConnectionStatusValues() {
        assertThat(ConnectionStatus.values()).hasSize(3);
    }

    @Test
    void testPendingReassignmentCanTransitionToActive() {
        assertThat(ConnectionStatus.PENDING_REASSIGNMENT.canTransitionTo(ConnectionStatus.ACTIVE)).isTrue();
    }

    @Test
    void testPendingReassignmentCanTransitionToRevoked() {
        assertThat(ConnectionStatus.PENDING_REASSIGNMENT.canTransitionTo(ConnectionStatus.REVOKED)).isTrue();
    }

    @Test
    void testPendingReassignmentCannotTransitionToPendingReassignment() {
        assertThat(
            ConnectionStatus.PENDING_REASSIGNMENT.canTransitionTo(ConnectionStatus.PENDING_REASSIGNMENT)).isFalse();
    }

    @Test
    void testRevokedCannotTransitionToActive() {
        assertThat(ConnectionStatus.REVOKED.canTransitionTo(ConnectionStatus.ACTIVE)).isFalse();
    }

    @Test
    void testRevokedCannotTransitionToPendingReassignment() {
        assertThat(ConnectionStatus.REVOKED.canTransitionTo(ConnectionStatus.PENDING_REASSIGNMENT)).isFalse();
    }

    @Test
    void testRevokedCannotTransitionToRevoked() {
        assertThat(ConnectionStatus.REVOKED.canTransitionTo(ConnectionStatus.REVOKED)).isFalse();
    }
}
