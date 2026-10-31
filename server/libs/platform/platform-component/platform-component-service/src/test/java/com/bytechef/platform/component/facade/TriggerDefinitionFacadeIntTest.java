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

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.component.config.ComponentRegistryConfiguration;
import com.bytechef.platform.component.config.ComponentRegistryConfigurationSharedMocks;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class TriggerDefinitionFacadeIntTest {

    private static final String WORKFLOW_EXECUTION_ID = "workflowExecutionId";

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private FacadeTestComponentHandler facadeTestComponentHandler;

    @Autowired
    private TriggerDefinitionFacade triggerDefinitionFacade;

    private Connection connection;

    @BeforeEach
    void beforeEach() {
        facadeTestComponentHandler.clearInvocations();

        connection = connectionRepository.save(createConnection());
    }

    @AfterEach
    void afterEach() {
        connectionRepository.deleteAll();
    }

    @Test
    void testPerformDynamicWebhookDisable() {
        triggerDefinitionFacade.executeWebhookDisable(
            COMPONENT_NAME, 1, "newEvent", Map.of(), WORKFLOW_EXECUTION_ID, Map.of("subscription", "sub1"),
            connection.getId());

        assertThat(facadeTestComponentHandler.getInvocations()).containsExactly("webhookDisable:sub1@C1");
    }

    @Test
    void testPerformDynamicWebhookEnable() {
        WebhookEnableOutput webhookEnableOutput = triggerDefinitionFacade.executeWebhookEnable(
            COMPONENT_NAME, 1, "newEvent", Map.of(), WORKFLOW_EXECUTION_ID, connection.getId(),
            "https://localhost/webhooks", 0L);

        Map<String, Object> parameters = Map.copyOf(webhookEnableOutput.parameters());

        assertThat(parameters).containsEntry("subscription", "C1_https://localhost/webhooks");
        assertThat(facadeTestComponentHandler.getInvocations()).containsExactly("webhookEnable@C1");
    }

    @Test
    void testPerformDynamicWebhookRefresh() {
        WebhookEnableOutput webhookEnableOutput = triggerDefinitionFacade.executeDynamicWebhookRefresh(
            COMPONENT_NAME, 1, "newEvent", Map.of("subscription", "sub1"), connection.getId());

        Map<String, Object> parameters = Map.copyOf(webhookEnableOutput.parameters());

        assertThat(parameters).containsEntry("subscription", "C1_refreshed_sub1");
    }

    @Test
    void testPerformListenerDisable() {
        triggerDefinitionFacade.executeListenerDisable(
            COMPONENT_NAME, 1, "listen", Map.of(), WORKFLOW_EXECUTION_ID, connection.getId());

        assertThat(facadeTestComponentHandler.getInvocations())
            .containsExactly("listenerDisable:" + WORKFLOW_EXECUTION_ID + "@C1");
    }

    @Test
    void testPerformListenerEnable() {
        triggerDefinitionFacade.executeListenerEnable(
            COMPONENT_NAME, 1, "listen", Map.of(), WORKFLOW_EXECUTION_ID, connection.getId());

        assertThat(facadeTestComponentHandler.getInvocations())
            .containsExactly("listenerEnable:" + WORKFLOW_EXECUTION_ID + "@C1");
    }

    @Test
    void testPerformListenerEnableWithoutConnection() {
        triggerDefinitionFacade.executeListenerEnable(
            COMPONENT_NAME, 1, "listen", Map.of(), WORKFLOW_EXECUTION_ID, null);

        assertThat(facadeTestComponentHandler.getInvocations())
            .containsExactly("listenerEnable:" + WORKFLOW_EXECUTION_ID + "@none");
    }

    private static Connection createConnection() {
        return Connection.builder()
            .authorizationType(AuthorizationType.API_KEY)
            .componentName(COMPONENT_NAME)
            .connectionVersion(1)
            .name("Facade Test Connection")
            .parameters(new HashMap<>(Map.of(CHANNEL_ID, "C1")))
            .type(PlatformType.AUTOMATION)
            .build();
    }
}
