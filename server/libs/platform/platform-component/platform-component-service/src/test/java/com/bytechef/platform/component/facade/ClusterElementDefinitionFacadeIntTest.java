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

import static com.bytechef.platform.component.facade.FacadeTestComponentHandler.CHANNEL_ID;
import static com.bytechef.platform.component.facade.FacadeTestComponentHandler.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.config.ComponentRegistryConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    ComponentRegistryConfiguration.class, FacadeTestComponentHandler.class
})
@ComponentRegistryConfigurationSharedMocks
class ClusterElementDefinitionFacadeIntTest {

    private static final String CLUSTER_ELEMENT_NAME = "probe";
    private static final String PROPERTY_NAME = "schemas";
    private static final String SOURCE_WORKFLOW_NODE_NAME = "source_1";

    private static final Map<String, Object> RESOLVE_INPUT_PARAMETERS = Map.of("resolve", true);
    private static final Map<String, Object> SOURCE_EXTENSIONS = Map.of(
        "clusterElements",
        Map.of(
            "source", Map.of("name", SOURCE_WORKFLOW_NODE_NAME, "type", COMPONENT_NAME + "/v1/source")));
    private static final Map<String, Map<String, ?>> SOURCE_INPUT_PARAMETERS = Map.of(
        SOURCE_WORKFLOW_NODE_NAME, Map.of("label", "sourceLabel"));

    @Autowired
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @AfterEach
    void afterEach() {
        connectionRepository.deleteAll();
    }

    @Test
    void testExecuteDynamicPropertiesWithResolverDelegatesToServiceWithResolver() {
        List<Property> properties = clusterElementDefinitionFacade.executeDynamicProperties(
            COMPONENT_NAME, 1, CLUSTER_ELEMENT_NAME, PROPERTY_NAME, RESOLVE_INPUT_PARAMETERS, SOURCE_EXTENSIONS,
            List.of(), null, Map.of(), SOURCE_INPUT_PARAMETERS);

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("none_sourceLabel_none");
    }

    @Test
    void testExecuteDynamicPropertiesWithResolverResolvesConnectionId() {
        Connection connection = saveConnection("C1");

        List<Property> properties = clusterElementDefinitionFacade.executeDynamicProperties(
            COMPONENT_NAME, 1, CLUSTER_ELEMENT_NAME, PROPERTY_NAME, Map.of(), Map.of(), List.of(), connection.getId(),
            Map.of(), Map.of());

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("C1_unresolved");
    }

    @Test
    void testExecuteDynamicPropertiesWithResolverResolvesClusterElementConnections() {
        Connection sourceConnection = saveConnection("S1");
        Connection destinationConnection = saveConnection("D1");

        List<Property> properties = clusterElementDefinitionFacade.executeDynamicProperties(
            COMPONENT_NAME, 1, CLUSTER_ELEMENT_NAME, PROPERTY_NAME, RESOLVE_INPUT_PARAMETERS, SOURCE_EXTENSIONS,
            List.of(), null,
            Map.of(
                SOURCE_WORKFLOW_NODE_NAME, sourceConnection.getId(), "destination_1", destinationConnection.getId()),
            SOURCE_INPUT_PARAMETERS);

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("none_sourceLabel_S1");

        long missingConnectionId = destinationConnection.getId() + 1000;
        Map<String, Long> clusterElementConnectionIds = Map.of(
            SOURCE_WORKFLOW_NODE_NAME, sourceConnection.getId(), "destination_1", missingConnectionId);

        assertThatThrownBy(() -> clusterElementDefinitionFacade.executeDynamicProperties(
            COMPONENT_NAME, 1, CLUSTER_ELEMENT_NAME, PROPERTY_NAME, RESOLVE_INPUT_PARAMETERS, SOURCE_EXTENSIONS,
            List.of(), null, clusterElementConnectionIds, SOURCE_INPUT_PARAMETERS))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testExecuteDynamicPropertiesWithoutConnectionIdPassesNullConnection() {
        saveConnection("C1");

        List<Property> properties = clusterElementDefinitionFacade.executeDynamicProperties(
            COMPONENT_NAME, 1, CLUSTER_ELEMENT_NAME, PROPERTY_NAME, Map.of(), Map.of(), List.of(), null, Map.of(),
            Map.of());

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("none_unresolved");
    }

    @Test
    void testExecuteDynamicPropertiesSimpleOverloadDelegatesToService() {
        Connection connection = saveConnection("C1");

        List<Property> properties = clusterElementDefinitionFacade.executeDynamicProperties(
            COMPONENT_NAME, 1, CLUSTER_ELEMENT_NAME, PROPERTY_NAME, Map.of(), List.of(), connection.getId());

        assertThat(properties)
            .extracting(Property::getName)
            .containsExactly("C1_unresolved");
    }

    private Connection saveConnection(String channelId) {
        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .name("Facade Test Connection " + channelId)
            .parameters(new HashMap<>(Map.of(CHANNEL_ID, channelId)))
            .type(PlatformType.AUTOMATION)
            .build();

        return connectionRepository.save(connection);
    }
}
