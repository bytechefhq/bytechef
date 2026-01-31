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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
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
}
