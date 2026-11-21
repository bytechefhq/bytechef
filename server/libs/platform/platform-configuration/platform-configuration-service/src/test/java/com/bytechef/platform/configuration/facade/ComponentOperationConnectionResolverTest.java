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

package com.bytechef.platform.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.ClusterElementNode;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.ComponentOperationConnectionResolver.ResolvedConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ComponentOperationConnectionResolverTest {

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private ComponentOperationConnectionResolver componentOperationConnectionResolver;

    @BeforeEach
    void setUp() {
        componentOperationConnectionResolver = new ComponentOperationConnectionResolver(
            componentConnectionFacade, connectionService, workflowTestConfigurationService);
    }

    @Test
    void testResolveReturnsTheBoundConnectionOfTheComponent() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(new ComponentConnection("github", 1, "script_1", "conn", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "script_1", 1L))
            .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "conn", "script_1")));
        Connection boundConnection = connectionOf("github");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));

        assertEquals(
            Optional.of(new ResolvedConnection(100L)),
            componentOperationConnectionResolver.resolve("workflow1", "script_1", null, "github", "conn", 1L));
    }

    @Test
    void testResolveRejectsABindingLeftOverFromAnotherComponent() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(new ComponentConnection("github", 1, "script_1", "conn", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "script_1", 1L))
            .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "conn", "script_1")));
        Connection boundConnection = connectionOf("slack");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));

        assertTrue(
            componentOperationConnectionResolver.resolve("workflow1", "script_1", null, "github", "conn", 1L)
                .isEmpty());
    }

    @Test
    void testResolveRejectsAClusterElementBindingSharedWithAnotherComponent() {
        when(componentConnectionFacade.getClusterElementComponentConnections(
            "workflow1", "aiAgent_1", "tools", "github_1"))
                .thenReturn(List.of(new ComponentConnection("github", 1, "github_1", "shared", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "aiAgent_1", 1L))
            .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "shared", "aiAgent_1")));
        Connection boundConnection = connectionOf("slack");

        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.of(boundConnection));

        assertTrue(
            componentOperationConnectionResolver.resolve(
                "workflow1", "aiAgent_1", new ClusterElementNode("tools", "github_1"), "github", null, 1L)
                .isEmpty());
    }

    @Test
    void testResolveReturnsEmptyWhenTheBoundConnectionNoLongerExists() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "script_1"))
            .thenReturn(List.of(new ComponentConnection("github", 1, "script_1", "conn", true)));
        when(workflowTestConfigurationService.getWorkflowTestConfigurationConnections("workflow1", "script_1", 1L))
            .thenReturn(List.of(new WorkflowTestConfigurationConnection(100L, "conn", "script_1")));
        when(connectionService.fetchConnection(100L))
            .thenReturn(Optional.empty());

        assertTrue(
            componentOperationConnectionResolver.resolve("workflow1", "script_1", null, "github", "conn", 1L)
                .isEmpty());
    }

    @Test
    void testResolveReturnsEmptyForANodeThatIsNotSaved() {
        when(componentConnectionFacade.getWorkflowNodeComponentConnections("workflow1", "unsaved_1"))
            .thenThrow(new IllegalArgumentException("Workflow task with name: unsaved_1 does not exist"));

        assertTrue(
            componentOperationConnectionResolver.resolve("workflow1", "unsaved_1", null, "github", null, 1L)
                .isEmpty());
    }

    private static Connection connectionOf(String componentName) {
        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn(componentName);

        return connection;
    }
}
