/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestSupport;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.ee.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.ai.tool.util.ToolPropertyUtils;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @version ee
 */
@SpringBootTest(classes = EmbeddedExecutionIntTestConfiguration.class)
class ToolFacadeIntTest {

    @Autowired
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private EmbeddedExecutionIntTestSupport embeddedExecutionIntTestSupport;

    @Autowired
    private ToolFacade toolFacade;

    @Autowired
    private ToolInvocationLogService toolInvocationLogService;

    @BeforeEach
    void beforeEach() {
        reset(clusterElementDefinitionFacade, clusterElementDefinitionService, componentDefinitionService);
    }

    @AfterEach
    void afterEach() {
        embeddedExecutionIntTestSupport.deleteAll();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecuteToolInjectsReservedContextParameters() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        embeddedExecutionIntTestSupport.createIntegrationInstance(connectedUserId, "slack", Environment.PRODUCTION, 7L);

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
    void testExecuteToolIsRecordedAsAnEmbeddedApiToolInvocationOfTheConnectedUser() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        IntegrationInstance integrationInstance = embeddedExecutionIntTestSupport.createIntegrationInstance(
            connectedUserId, "slack", Environment.PRODUCTION, 7L);

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), any(), eq(7L)))
            .thenReturn("ok");

        toolFacade.executeTool(
            "user-1", "slack_send", Map.of(), integrationInstance.getId(), Environment.PRODUCTION);

        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> {
                assertThat(toolInvocationLog.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.EMBEDDED_API_TOOL);
                assertThat(toolInvocationLog.getKind()).isEqualTo(ToolExecutionKind.COMPONENT);
                assertThat(toolInvocationLog.getToolName()).isEqualTo("slack_send");
                assertThat(toolInvocationLog.getComponentName()).isEqualTo("slack");
                assertThat(toolInvocationLog.getOperationName()).isEqualTo("send");
                assertThat(toolInvocationLog.getConnectionId()).isEqualTo(7L);
                assertThat(toolInvocationLog.getEnvironment()).isEqualTo(Environment.PRODUCTION.ordinal());
                assertThat(toolInvocationLog.getExternalUserId()).isEqualTo("user-1");
                assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(toolInvocationLog.getIntegrationInstanceId()).isEqualTo(integrationInstance.getId());
                assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
            });
    }

    @Test
    void testFailedExecuteToolIsRecordedAsAnError() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(null);

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), any(), isNull()))
            .thenThrow(new IllegalStateException("Channel not found"));

        assertThatThrownBy(
            () -> toolFacade.executeTool("user-1", "slack_send", Map.of(), null, Environment.PRODUCTION))
                .hasMessage("Channel not found");

        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> {
                assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.ERROR);
                assertThat(toolInvocationLog.getErrorType()).isEqualTo("IllegalStateException");
                assertThat(toolInvocationLog.getErrorMessage()).isEqualTo("Channel not found");
                assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(toolInvocationLog.getIntegrationInstanceId()).isNull();
            });
    }

    @Test
    void testExecuteToolWithoutTheRequiredConnectionIsRecordedAsConnectionRequired() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(connection(true));

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), any(), isNull()))
            .thenThrow(new IllegalStateException("Connection is required"));

        assertThatThrownBy(
            () -> toolFacade.executeTool("user-1", "slack_send", Map.of(), null, Environment.PRODUCTION))
                .hasMessage("Connection is required");

        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> {
                assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.CONNECTION_REQUIRED);
                assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.EMBEDDED_API_TOOL);
                assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(toolInvocationLog.getConnectionId()).isNull();
                assertThat(toolInvocationLog.getErrorType()).isNull();
            });
    }

    @Test
    void testExecuteToolOfAConnectionFreeComponentWithoutAConnectionRunsAndIsRecordedAsASuccess() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(null);

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), any(), isNull()))
            .thenReturn("ok");

        Object result = toolFacade.executeTool("user-1", "slack_send", Map.of(), null, Environment.PRODUCTION);

        assertEquals("ok", result);
        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> assertThat(toolInvocationLog.getOutcome())
                .isEqualTo(ToolExecutionOutcome.SUCCESS));
    }

    @Test
    void testExecuteToolOfAComponentWithAnOptionalConnectionWithoutAConnectionRunsAndIsRecordedAsASuccess() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(connection(false));

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), any(), isNull()))
            .thenReturn("ok");

        Object result = toolFacade.executeTool("user-1", "slack_send", Map.of(), null, Environment.PRODUCTION);

        assertEquals("ok", result);
        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> assertThat(toolInvocationLog.getOutcome())
                .isEqualTo(ToolExecutionOutcome.SUCCESS));
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

    private List<ToolInvocationLog> getToolInvocationLogs(long connectedUserId) {
        return toolInvocationLogService
            .getToolInvocationLogs(
                PlatformType.EMBEDDED, null, null, null, null, connectedUserId, null, null, null, 0)
            .getContent();
    }

    private void stubComponentDefinition(@Nullable ModifiableConnectionDefinition connectionDefinition) {
        ModifiableComponentDefinition modifiableComponentDefinition = ComponentDsl.component("slack");

        if (connectionDefinition != null) {
            modifiableComponentDefinition.connection(connectionDefinition);
        }

        when(componentDefinitionService.getComponentDefinition("slack", null))
            .thenReturn(new ComponentDefinition(modifiableComponentDefinition));
    }

    private static ModifiableConnectionDefinition connection(boolean authorizationRequired) {
        return ComponentDsl.connection()
            .authorizationRequired(authorizationRequired);
    }

    private void stubConnectedUserSlackIntegration(List<? extends Property> properties) {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        embeddedExecutionIntTestSupport.createIntegrationInstance(connectedUserId, "slack", Environment.PRODUCTION, 7L);

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
