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

package com.bytechef.automation.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.credential.store.CredentialStoreType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    ConnectionGraphQlController.class
})
@GraphQlTest(
    controllers = ConnectionGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
public class ConnectionGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Test
    void testDisconnectConnection() {
        // Given
        long connectionId = 123L;

        doNothing().when(workspaceConnectionFacade)
            .disconnectConnection(anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    disconnectConnection(connectionId: 123)
                }
                """)
            .execute()
            .path("disconnectConnection")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceConnectionFacade).disconnectConnection(connectionId);
    }

    @Test
    void testDisconnectConnectionWithVariable() {
        // Given
        long connectionId = 456L;

        doNothing().when(workspaceConnectionFacade)
            .disconnectConnection(anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation DisconnectConnection($connectionId: ID!) {
                    disconnectConnection(connectionId: $connectionId)
                }
                """)
            .variable("connectionId", connectionId)
            .execute()
            .path("disconnectConnection")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceConnectionFacade).disconnectConnection(connectionId);
    }

    @Test
    void testRegisterExistingConnectionMutation() {
        // Given
        when(workspaceConnectionFacade.registerExisting(
            eq(1L), any(ConnectionDTO.class), eq(CredentialStoreType.HASHICORP_VAULT), anyString()))
                .thenReturn(42L);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    registerExistingConnection(input: {
                        componentName: "acme-api"
                        connectionVersion: 1
                        credentialRef: "secret/data/bytechef/connections/abc-uuid"
                        credentialStoreType: HASHICORP_VAULT
                        environmentId: "1"
                        name: "vault-backed conn"
                        workspaceId: "1"
                    })
                }
                """)
            .execute()
            .path("registerExistingConnection")
            .entity(Long.class)
            .isEqualTo(42L);

        verify(workspaceConnectionFacade).registerExisting(
            eq(1L), any(ConnectionDTO.class), eq(CredentialStoreType.HASHICORP_VAULT),
            eq("secret/data/bytechef/connections/abc-uuid"));
    }

    @Test
    void testUpdateConnectionCredentials() {
        // Given
        doNothing().when(workspaceConnectionFacade)
            .updateConnectionCredentials(anyLong(), any(), anyInt());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateConnectionCredentials(input: {
                        connectionId: "123"
                        parameters: {apiKey: "new"}
                        version: 3
                    })
                }
                """)
            .execute()
            .path("updateConnectionCredentials")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceConnectionFacade).updateConnectionCredentials(123L, Map.of("apiKey", "new"), 3);
    }
}
