/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.ee.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.platform.ai.tool.util.ToolPropertyUtils;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 */
class ToolFacadeImplTest {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final ConnectionIdHelper connectionIdHelper = mock(ConnectionIdHelper.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);

    private final ToolFacadeImpl toolFacade = new ToolFacadeImpl(
        clusterElementDefinitionFacade, clusterElementDefinitionService, componentDefinitionService,
        connectedUserService, connectionIdHelper, integrationInstanceConfigurationService, integrationInstanceService,
        integrationService);

    @Test
    @SuppressWarnings("unchecked")
    void testExecuteToolInjectsReservedContextParameters() {
        when(connectionIdHelper.getConnectionId("user-1", "slack", null, Environment.PRODUCTION)).thenReturn(7L);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), captor.capture(), eq(7L)))
            .thenReturn("ok");

        Map<String, Object> input = new HashMap<>();

        input.put("text", "hi");

        Object result = toolFacade.executeTool("user-1", "slack_send", input, null, Environment.PRODUCTION);

        assertEquals("ok", result);

        Map<String, ?> passed = captor.getValue();

        assertEquals("user-1", passed.get(EmbeddedToolConstants.EXTERNAL_USER_ID));
        assertEquals("PRODUCTION", passed.get(EmbeddedToolConstants.ENVIRONMENT));
        assertEquals("hi", passed.get("text"));
    }

    @Test
    void testGetToolsExcludesToolOverrideProperties() {
        Property textProperty = mock(Property.class);

        when(textProperty.getName()).thenReturn("text");
        when(textProperty.getType()).thenReturn(com.bytechef.component.definition.Property.Type.STRING);
        when(textProperty.getRequired()).thenReturn(true);

        stubConnectedUserSlackIntegration(
            List.of(
                ToolPropertyUtils.toolNameProperty("tool"), ToolPropertyUtils.toolDescriptionProperty("tool"),
                textProperty));

        Map<String, List<ToolDTO>> tools = toolFacade.getTools(
            "user-1", List.of(), List.of(), List.of(), Environment.PRODUCTION);

        List<ToolDTO> slackTools = tools.get("slack");

        assertEquals(1, slackTools.size());

        ToolDTO toolDTO = slackTools.getFirst();

        assertEquals("SLACK_SEND_CHANNEL_MESSAGE", toolDTO.name());

        String parameters = toolDTO.parameters();

        assertTrue(parameters.contains("text"), parameters);
        assertFalse(parameters.contains("toolName"), parameters);
        assertFalse(parameters.contains("toolDescription"), parameters);
    }

    private void stubConnectedUserSlackIntegration(List<? extends Property> properties) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getIntegrationInstanceConfigurationId()).thenReturn(2L);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(1L, Environment.PRODUCTION))
            .thenReturn(List.of(integrationInstance));

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            mock(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfiguration.getIntegrationId()).thenReturn(3L);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(2L)))
            .thenReturn(List.of(integrationInstanceConfiguration));

        Integration integration = mock(Integration.class);

        when(integration.getComponentName()).thenReturn("slack");
        when(integrationService.getIntegrations(List.of(3L))).thenReturn(List.of(integration));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn("slack");
        when(componentDefinition.getVersion()).thenReturn(1);
        when(componentDefinitionService.getComponentDefinition("slack", null)).thenReturn(componentDefinition);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("slack");
        when(clusterElementDefinition.getName()).thenReturn("sendChannelMessage");
        when(clusterElementDefinition.getDescription()).thenReturn("Sends a message to a public channel.");
        when(clusterElementDefinition.getProperties()).thenAnswer(invocation -> properties);
        when(clusterElementDefinitionService.getClusterElementDefinitions("slack", 1, BaseToolFunction.TOOLS))
            .thenReturn(List.of(clusterElementDefinition));
    }
}
