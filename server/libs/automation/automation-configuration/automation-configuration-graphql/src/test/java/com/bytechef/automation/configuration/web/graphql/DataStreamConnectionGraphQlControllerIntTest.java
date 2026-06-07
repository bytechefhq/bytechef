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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests for {@link DataStreamConnectionGraphQlController}. Mocks the workspace connection facade and
 * component definition service, and verifies the connection picker correctly filters connections to those whose
 * component exposes at least one {@link ItemReader#SOURCE} cluster element.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class,
    DataStreamConnectionGraphQlController.class
})
@GraphQlTest(
    controllers = DataStreamConnectionGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
class DataStreamConnectionGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Test
    void testReturnsConnectionWithItemReader() {
        ConnectionDTO connection = newConnectionDTO(101L, "github-prod", "github", 1);

        when(workspaceConnectionFacade.getConnections(10L, null, null, 1L, null))
            .thenReturn(List.of(connection));

        ComponentDefinition componentDefinition = newComponentDefinitionWithItemReader("github", 1);

        when(componentDefinitionService.getConnectionComponentDefinition("github", 1)).thenReturn(componentDefinition);
        when(componentDefinitionService.getComponentDefinition("github", 1)).thenReturn(componentDefinition);

        graphQlTester
            .document("""
                query {
                    dataStreamCompatibleConnections(workspaceId: "10", environmentId: "1") {
                        id
                        name
                        componentName
                        componentVersion
                    }
                }
                """)
            .execute()
            .path("dataStreamCompatibleConnections")
            .entityList(Object.class)
            .hasSize(1)
            .path("dataStreamCompatibleConnections[0].id")
            .entity(String.class)
            .isEqualTo("101")
            .path("dataStreamCompatibleConnections[0].componentName")
            .entity(String.class)
            .isEqualTo("github");
    }

    @Test
    void testFiltersOutConnectionsWithoutItemReader() {
        ConnectionDTO incompatible = newConnectionDTO(202L, "no-readers", "boring", 1);

        when(workspaceConnectionFacade.getConnections(10L, null, null, 1L, null))
            .thenReturn(List.of(incompatible));

        ComponentDefinition componentDefinition = newComponentDefinitionWithoutItemReader("boring", 1);

        when(componentDefinitionService.getConnectionComponentDefinition("boring", 1)).thenReturn(componentDefinition);
        when(componentDefinitionService.getComponentDefinition("boring", 1)).thenReturn(componentDefinition);

        graphQlTester
            .document("""
                query {
                    dataStreamCompatibleConnections(workspaceId: "10", environmentId: "1") {
                        id
                    }
                }
                """)
            .execute()
            .path("dataStreamCompatibleConnections")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testFiltersOutConnectionWithListActionOnly() {
        // After the LIST_ACTION strategy was dropped, list*-only components no longer qualify; only ItemReader does.
        ConnectionDTO connection = newConnectionDTO(303L, "list-only", "hubspot", 1);

        when(workspaceConnectionFacade.getConnections(10L, null, null, 1L, null))
            .thenReturn(List.of(connection));

        ComponentDefinition componentDefinition = newComponentDefinitionWithoutItemReader("hubspot", 1);

        when(componentDefinitionService.getConnectionComponentDefinition("hubspot", 1))
            .thenReturn(componentDefinition);
        when(componentDefinitionService.getComponentDefinition("hubspot", 1)).thenReturn(componentDefinition);

        graphQlTester
            .document("""
                query {
                    dataStreamCompatibleConnections(workspaceId: "10", environmentId: "1") {
                        id
                    }
                }
                """)
            .execute()
            .path("dataStreamCompatibleConnections")
            .entityList(Object.class)
            .hasSize(0);
    }

    private static ConnectionDTO newConnectionDTO(Long id, String name, String componentName, int connectionVersion) {
        return ConnectionDTO.builder()
            .id(id)
            .name(name)
            .componentName(componentName)
            .connectionVersion(connectionVersion)
            .build();
    }

    private static ComponentDefinition newComponentDefinitionWithItemReader(String name, int version) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(name);
        when(componentDefinition.getVersion()).thenReturn(version);

        ClusterElementDefinition itemReaderElement = mock(ClusterElementDefinition.class);

        when(itemReaderElement.getType()).thenReturn(ItemReader.SOURCE);

        when(componentDefinition.getClusterElements()).thenReturn(List.of(itemReaderElement));

        return componentDefinition;
    }

    private static ComponentDefinition newComponentDefinitionWithoutItemReader(String name, int version) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(name);
        when(componentDefinition.getVersion()).thenReturn(version);

        ClusterElementDefinition unrelatedElement = mock(ClusterElementDefinition.class);
        ClusterElementType unrelated = new ClusterElementType("OTHER", "other", "Other");

        when(unrelatedElement.getType()).thenReturn(unrelated);

        when(componentDefinition.getClusterElements()).thenReturn(List.of(unrelatedElement));

        return componentDefinition;
    }
}
