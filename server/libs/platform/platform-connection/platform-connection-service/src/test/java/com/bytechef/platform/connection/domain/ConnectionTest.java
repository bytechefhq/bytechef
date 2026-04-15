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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionTest {

    @Test
    void testSetStatusFromActiveToPendingReassignment() {
        Connection connection = new Connection();

        connection.setStatus(ConnectionStatus.PENDING_REASSIGNMENT);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.PENDING_REASSIGNMENT);
    }

    @Test
    void testSetStatusFromActiveToRevoked() {
        Connection connection = new Connection();

        connection.setStatus(ConnectionStatus.REVOKED);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.REVOKED);
    }

    @Test
    void testSetStatusFromRevokedToActiveThrows() {
        Connection connection = new Connection();

        connection.setStatus(ConnectionStatus.REVOKED);

        assertThatThrownBy(() -> connection.setStatus(ConnectionStatus.ACTIVE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot transition connection status from REVOKED to ACTIVE");
    }

    @Test
    void testSetStatusSameStatusIsIdempotent() {
        Connection connection = new Connection();

        connection.setStatus(ConnectionStatus.ACTIVE);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
    }

    @Test
    void testSetStatusFromPendingReassignmentToActive() {
        Connection connection = new Connection();

        connection.setStatus(ConnectionStatus.PENDING_REASSIGNMENT);

        connection.setStatus(ConnectionStatus.ACTIVE);

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
    }

    @Test
    void testGetStatusWithDefaultConstructor() {
        Connection connection = new Connection();

        assertThat(connection.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
    }

    @Test
    void testGetVisibilityWithDefaultConstructor() {
        Connection connection = new Connection();

        assertThat(connection.getVisibility()).isEqualTo(ConnectionVisibility.PRIVATE);
    }
}
