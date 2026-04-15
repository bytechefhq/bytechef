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
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.config.ComponentRegistryConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
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
class ActionDefinitionFacadeConnectionStatusIntTest {

    private static final String ACTION_NAME = "send";
    private static final String CONNECTION_NAME = "Facade Test Connection";
    private static final String PROPERTY_NAME = "channel";

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ConnectionRepository connectionRepository;

    @AfterEach
    void afterEach() {
        connectionRepository.deleteAll();
    }

    @Test
    void testExecuteOptionsWithActiveConnectionPassesTheStoredConnection() {
        Connection connection = saveConnection(ConnectionStatus.ACTIVE);

        List<Option> options = actionDefinitionFacade.executeOptions(
            COMPONENT_NAME, 1, ACTION_NAME, PROPERTY_NAME, Map.of(), List.of(), null, connection.getId());

        assertThat(options)
            .extracting(Option::getValue)
            .containsExactly("C1");
    }

    @Test
    void testExecuteOptionsWithPendingReassignmentConnectionThrows() {
        Connection connection = saveConnection(ConnectionStatus.PENDING_REASSIGNMENT);

        assertThatThrownBy(
            () -> actionDefinitionFacade.executeOptions(
                COMPONENT_NAME, 1, ACTION_NAME, PROPERTY_NAME, Map.of(), List.of(), null, connection.getId()))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(CONNECTION_NAME)
                    .hasMessageContaining("PENDING_REASSIGNMENT");
    }

    @Test
    void testExecuteOptionsWithRevokedConnectionThrows() {
        Connection connection = saveConnection(ConnectionStatus.REVOKED);

        assertThatThrownBy(
            () -> actionDefinitionFacade.executeOptions(
                COMPONENT_NAME, 1, ACTION_NAME, PROPERTY_NAME, Map.of(), List.of(), null, connection.getId()))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(CONNECTION_NAME)
                    .hasMessageContaining("REVOKED");
    }

    @Test
    void testExecuteOptionsWithoutConnectionIdPassesNoConnection() {
        saveConnection(ConnectionStatus.REVOKED);

        List<Option> options = actionDefinitionFacade.executeOptions(
            COMPONENT_NAME, 1, ACTION_NAME, PROPERTY_NAME, Map.of(), List.of(), null, null);

        assertThat(options).isEmpty();
    }

    private Connection saveConnection(ConnectionStatus connectionStatus) {
        Connection connection = Connection.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .name(CONNECTION_NAME)
            .parameters(new HashMap<>(Map.of(CHANNEL_ID, "C1")))
            .status(connectionStatus)
            .type(PlatformType.AUTOMATION)
            .build();

        return connectionRepository.save(connection);
    }
}
