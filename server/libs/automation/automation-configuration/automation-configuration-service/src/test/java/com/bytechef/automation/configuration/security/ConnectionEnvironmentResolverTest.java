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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * An absent connection has no environment, but a failure must reach the permission check, which denies, rather than be
 * turned into "no environment", which falls back to the member's roles across every environment.
 *
 * @author Ivica Cardic
 */
class ConnectionEnvironmentResolverTest {

    private static final long CONNECTION_ID = 7L;

    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final ConnectionEnvironmentResolver resolver = new ConnectionEnvironmentResolver(connectionService);

    @Test
    void testReportsTheEnvironmentTheConnectionBelongsTo() {
        Connection connection = new Connection();

        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());

        when(connectionService.getConnections(List.of(CONNECTION_ID))).thenReturn(List.of(connection));

        assertThat(resolver.fetchEnvironment(CONNECTION_ID)).contains(Environment.PRODUCTION);
    }

    @Test
    void testAnUnknownConnectionHasNoEnvironment() {
        when(connectionService.getConnections(List.of(CONNECTION_ID))).thenReturn(List.of());

        assertThat(resolver.fetchEnvironment(CONNECTION_ID)).isEmpty();
    }

    @Test
    void testAnUnknownStoredEnvironmentFailsRatherThanReportingNone() {
        Connection connection = new Connection();

        connection.setEnvironmentId(99);

        when(connectionService.getConnections(List.of(CONNECTION_ID))).thenReturn(List.of(connection));

        assertThatThrownBy(() -> resolver.fetchEnvironment(CONNECTION_ID))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testALookupFailurePropagates() {
        when(connectionService.getConnections(List.of(CONNECTION_ID)))
            .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> resolver.fetchEnvironment(CONNECTION_ID))
            .isInstanceOf(IllegalStateException.class);
    }
}
