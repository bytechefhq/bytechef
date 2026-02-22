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

package com.bytechef.platform.component.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ClusterElementDefinitionFacadeTest {

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ContextFactory contextFactory;

    private ClusterElementDefinitionFacadeImpl clusterElementDefinitionFacade;

    @BeforeEach
    void setUp() {
        clusterElementDefinitionFacade = new ClusterElementDefinitionFacadeImpl(
            clusterElementDefinitionService, connectionService, contextFactory);
    }

    @Test
    void testExecuteDynamicPropertiesWithResolverDelegatesToServiceWithResolver() {
        String componentName = "fieldMapper";
        int componentVersion = 1;
        String clusterElementName = "map";
        String propertyName = "schemas";

        Map<String, Object> inputParameters = Map.of("key", "value");
        Map<String, Object> extensions = Map.of();
        List<String> lookupDependsOnPaths = List.of("useJsonSchema");
        Map<String, Long> clusterElementConnectionIds = Map.of();
        Map<String, Map<String, ?>> clusterElementInputParameters = Map.of();

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(clusterElementDefinitionService.executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), isNull(), any(ClusterElementResolverFunction.class)))
                .thenReturn(expectedProperties);

        List<Property> result = clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, inputParameters, extensions,
            lookupDependsOnPaths, null, clusterElementConnectionIds, clusterElementInputParameters);

        assertEquals(expectedProperties, result);

        ArgumentCaptor<ClusterElementResolverFunction> resolverCaptor =
            ArgumentCaptor.forClass(ClusterElementResolverFunction.class);

        verify(clusterElementDefinitionService).executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), isNull(), resolverCaptor.capture());

        assertNotNull(resolverCaptor.getValue());
    }

    @Test
    void testExecuteDynamicPropertiesWithResolverResolvesConnectionId() {
        String componentName = "fieldMapper";
        int componentVersion = 1;
        String clusterElementName = "map";
        String propertyName = "schemas";
        long connectionId = 100L;

        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn("fieldMapper");
        when(connection.getConnectionVersion()).thenReturn(1);
        when(connection.getParameters()).thenReturn(Map.of());
        when(connectionService.getConnection(connectionId)).thenReturn(connection);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(clusterElementDefinitionService.executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), any(ComponentConnection.class), any(ClusterElementResolverFunction.class)))
                .thenReturn(expectedProperties);

        List<Property> result = clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, Map.of(), Map.of(),
            List.of(), connectionId, Map.of(), Map.of());

        assertEquals(expectedProperties, result);

        verify(connectionService).getConnection(connectionId);
    }

    @Test
    void testExecuteDynamicPropertiesWithResolverResolvesClusterElementConnections() {
        String componentName = "fieldMapper";
        int componentVersion = 1;
        String clusterElementName = "map";
        String propertyName = "schemas";
        long sourceConnectionId = 200L;
        long destinationConnectionId = 300L;

        Connection sourceConnection = mock(Connection.class);

        when(sourceConnection.getComponentName()).thenReturn("csvFile");
        when(sourceConnection.getConnectionVersion()).thenReturn(1);
        when(sourceConnection.getParameters()).thenReturn(Map.of());

        Connection destinationConnection = mock(Connection.class);

        when(destinationConnection.getComponentName()).thenReturn("googleSheets");
        when(destinationConnection.getConnectionVersion()).thenReturn(1);
        when(destinationConnection.getParameters()).thenReturn(Map.of());

        when(connectionService.getConnection(sourceConnectionId)).thenReturn(sourceConnection);
        when(connectionService.getConnection(destinationConnectionId)).thenReturn(destinationConnection);

        Map<String, Long> clusterElementConnectionIds = Map.of(
            "csvFile_source_1", sourceConnectionId, "googleSheets_destination_1", destinationConnectionId);

        when(clusterElementDefinitionService.executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), isNull(), any(ClusterElementResolverFunction.class)))
                .thenReturn(List.of());

        clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, Map.of(), Map.of(),
            List.of(), null, clusterElementConnectionIds, Map.of());

        verify(connectionService).getConnection(sourceConnectionId);
        verify(connectionService).getConnection(destinationConnectionId);
    }

    @Test
    void testExecuteDynamicPropertiesWithoutConnectionIdPassesNullConnection() {
        String componentName = "fieldMapper";
        int componentVersion = 1;
        String clusterElementName = "map";
        String propertyName = "schemas";

        when(clusterElementDefinitionService.executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), isNull(), any(ClusterElementResolverFunction.class)))
                .thenReturn(List.of());

        clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, Map.of(), Map.of(),
            List.of(), null, Map.of(), Map.of());

        verify(connectionService, never()).getConnection(any(Long.class));

        verify(clusterElementDefinitionService).executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), isNull(), any(ClusterElementResolverFunction.class));
    }

    @Test
    void testExecuteDynamicPropertiesSimpleOverloadDelegatesToService() {
        String componentName = "openai";
        int componentVersion = 1;
        String clusterElementName = "chat";
        String propertyName = "model";
        long connectionId = 50L;

        Connection connection = mock(Connection.class);

        when(connection.getComponentName()).thenReturn("openai");
        when(connection.getConnectionVersion()).thenReturn(1);
        when(connection.getParameters()).thenReturn(Map.of());
        when(connectionService.getConnection(connectionId)).thenReturn(connection);

        List<Property> expectedProperties = List.of(mock(Property.class));

        when(clusterElementDefinitionService.executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), any(ComponentConnection.class)))
                .thenReturn(expectedProperties);

        List<Property> result = clusterElementDefinitionFacade.executeDynamicProperties(
            componentName, componentVersion, clusterElementName, propertyName, Map.of(), List.of(), connectionId);

        assertEquals(expectedProperties, result);

        verify(clusterElementDefinitionService).executeDynamicProperties(
            eq(componentName), eq(componentVersion), eq(clusterElementName), eq(propertyName),
            anyMap(), anyList(), any(ComponentConnection.class));
    }
}
