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
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.ai.tool.util.ToolPropertyUtils;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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
    private ExternalInvocationLogService externalInvocationLogService;

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

        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(externalInvocationLog.getSurface()).isEqualTo(ExternalInvocationSurface.EMBEDDED_API_TOOL);
                assertThat(externalInvocationLog.getKind()).isEqualTo(ExternalInvocationKind.COMPONENT);
                assertThat(externalInvocationLog.getToolName()).isEqualTo("slack_send");
                assertThat(externalInvocationLog.getComponentName()).isEqualTo("slack");
                assertThat(externalInvocationLog.getOperationName()).isEqualTo("send");
                assertThat(externalInvocationLog.getConnectionId()).isEqualTo(7L);
                assertThat(externalInvocationLog.getEnvironment()).isEqualTo(Environment.PRODUCTION.ordinal());
                assertThat(externalInvocationLog.getExternalUserId()).isEqualTo("user-1");
                assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationLog.getIntegrationInstanceId()).isEqualTo(integrationInstance.getId());
                assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.SUCCESS);
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

        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
                assertThat(externalInvocationLog.getErrorType()).isEqualTo("IllegalStateException");
                assertThat(externalInvocationLog.getErrorMessage()).isEqualTo("Channel not found");
                assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationLog.getIntegrationInstanceId()).isNull();
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

        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.CONNECTION_REQUIRED);
                assertThat(externalInvocationLog.getSurface()).isEqualTo(ExternalInvocationSurface.EMBEDDED_API_TOOL);
                assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationLog.getConnectionId()).isNull();
                assertThat(externalInvocationLog.getErrorType()).isNull();
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
        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> assertThat(externalInvocationLog.getOutcome())
                .isEqualTo(ExternalInvocationOutcome.SUCCESS));
    }

    @Test
    void testExecuteToolOfAComponentWithAnOptionalConnectionWithoutAConnectionRunsAndIsRecordedAsASuccess() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(connection(false));

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), any(), isNull()))
            .thenReturn("ok");

        Object result = toolFacade.executeTool("user-1", "slack_send", Map.of(), null, Environment.PRODUCTION);

        assertEquals("ok", result);
        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> assertThat(externalInvocationLog.getOutcome())
                .isEqualTo(ExternalInvocationOutcome.SUCCESS));
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

    private List<ExternalInvocationLog> getExternalInvocationLogs(long connectedUserId) {
        return externalInvocationLogService
            .getExternalInvocationLogs(
                PlatformType.EMBEDDED, null, null, null, null, null, connectedUserId, null, null, null, 0)
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
