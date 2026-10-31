/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.ee.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.platform.ai.tool.util.ToolPropertyUtils;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 */
@SpringBootTest(
    classes = EmbeddedExecutionIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
class ToolFacadeIntTest {

    private static final long CONNECTION_ID = 7L;

    @MockitoBean
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @MockitoBean
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserRepository connectedUserRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceRepository integrationInstanceRepository;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ToolFacade toolFacade;

    @AfterEach
    void afterEach() {
        integrationInstanceRepository.deleteAll();
        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
        connectedUserRepository.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecuteToolInjectsReservedContextParameters() {
        createConnectedUserSlackIntegration();

        ArgumentCaptor<Map<String, ?>> parametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        when(
            clusterElementDefinitionFacade.executeTool(
                eq("slack"), eq("send"), parametersArgumentCaptor.capture(), eq(CONNECTION_ID)))
                    .thenReturn("ok");

        Map<String, Object> input = new HashMap<>();

        input.put("text", "hi");

        Object result = toolFacade.executeTool("user-1", "slack_send", input, null, Environment.PRODUCTION);

        assertThat(result).isEqualTo("ok");

        Map<String, ?> parameters = parametersArgumentCaptor.getValue();

        assertThat(parameters.get(EmbeddedToolConstants.EXTERNAL_USER_ID)).isEqualTo("user-1");
        assertThat(parameters.get(EmbeddedToolConstants.ENVIRONMENT)).isEqualTo("PRODUCTION");
        assertThat(parameters.get("text")).isEqualTo("hi");
    }

    @Test
    void testGetToolsExcludesToolOverrideProperties() {
        createConnectedUserSlackIntegration();

        Property textProperty = mock(Property.class);

        when(textProperty.getName()).thenReturn("text");
        when(textProperty.getType()).thenReturn(com.bytechef.component.definition.Property.Type.STRING);
        when(textProperty.getRequired()).thenReturn(true);

        stubSlackComponentDefinition(
            List.of(
                ToolPropertyUtils.toolNameProperty("tool"), ToolPropertyUtils.toolDescriptionProperty("tool"),
                textProperty));

        Map<String, List<ToolDTO>> tools = toolFacade.getTools(
            "user-1", List.of(), List.of(), List.of(), Environment.PRODUCTION);

        assertThat(tools).containsOnlyKeys("slack");

        List<ToolDTO> slackTools = tools.get("slack");

        assertThat(slackTools).hasSize(1);

        ToolDTO toolDTO = slackTools.getFirst();

        assertThat(toolDTO.name()).isEqualTo("SLACK_SEND_CHANNEL_MESSAGE");

        String parameters = toolDTO.parameters();

        assertThat(parameters).contains("text");
        assertThat(parameters).doesNotContain("toolName");
        assertThat(parameters).doesNotContain("toolDescription");
    }

    private void createConnectedUserSlackIntegration() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "user-1", Environment.PRODUCTION, null, null, Map.of());

        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("Slack");

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Slack configuration");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceService.create(connectedUser.getId(), CONNECTION_ID,
            integrationInstanceConfiguration.getId());
    }

    private void stubSlackComponentDefinition(List<? extends Property> properties) {
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
