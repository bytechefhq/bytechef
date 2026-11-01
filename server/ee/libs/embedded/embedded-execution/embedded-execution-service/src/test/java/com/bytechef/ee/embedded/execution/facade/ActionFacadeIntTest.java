/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestSupport;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EmbeddedExecutionIntTestConfiguration.class)
class ActionFacadeIntTest {

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ActionFacade actionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private EmbeddedExecutionIntTestSupport embeddedExecutionIntTestSupport;

    @Autowired
    private ToolInvocationLogService toolInvocationLogService;

    @BeforeEach
    void beforeEach() {
        reset(actionDefinitionFacade, componentDefinitionService);
    }

    @AfterEach
    void afterEach() {
        embeddedExecutionIntTestSupport.deleteAll();
    }

    @Test
    void testExecuteActionIsRecordedAsAnEmbeddedApiActionInvocationOfTheConnectedUser() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.STAGING);

        IntegrationInstance integrationInstance = embeddedExecutionIntTestSupport.createIntegrationInstance(
            connectedUserId, "slack", Environment.STAGING, 7L);

        when(
            actionDefinitionFacade.executePerform(
                eq("slack"), eq(1), eq("sendMessage"), any(), any(), any(), any(), any(), any(),
                eq(Map.of("slack", 7L)), any(), any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn("ok");

        Object result = actionFacade.executeAction(
            "user-1", "slack", 1, "sendMessage", Map.of("text", "hi"), integrationInstance.getId(),
            Environment.STAGING);

        assertThat(result).isEqualTo("ok");
        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> {
                assertThat(toolInvocationLog.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.EMBEDDED_API_ACTION);
                assertThat(toolInvocationLog.getKind()).isEqualTo(ToolExecutionKind.COMPONENT);
                assertThat(toolInvocationLog.getToolName()).isEqualTo("sendMessage");
                assertThat(toolInvocationLog.getComponentName()).isEqualTo("slack");
                assertThat(toolInvocationLog.getComponentVersion()).isEqualTo(1);
                assertThat(toolInvocationLog.getOperationName()).isEqualTo("sendMessage");
                assertThat(toolInvocationLog.getConnectionId()).isEqualTo(7L);
                assertThat(toolInvocationLog.getEnvironment()).isEqualTo(Environment.STAGING.ordinal());
                assertThat(toolInvocationLog.getExternalUserId()).isEqualTo("user-1");
                assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(toolInvocationLog.getIntegrationInstanceId()).isEqualTo(integrationInstance.getId());
                assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
            });
    }

    @Test
    void testFailedExecuteActionIsRecordedAsAnError() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(null);

        when(
            actionDefinitionFacade.executePerform(
                eq("slack"), eq(1), eq("sendMessage"), any(), any(), any(), any(), any(), any(), eq(Map.of()), any(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Channel not found"));

        assertThatThrownBy(
            () -> actionFacade.executeAction(
                "user-1", "slack", 1, "sendMessage", Map.of(), null, Environment.PRODUCTION))
                    .hasMessage("Channel not found");

        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> {
                assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.ERROR);
                assertThat(toolInvocationLog.getErrorType()).isEqualTo("IllegalStateException");
                assertThat(toolInvocationLog.getErrorMessage()).isEqualTo("Channel not found");
                assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(toolInvocationLog.getConnectionId()).isNull();
                assertThat(toolInvocationLog.getIntegrationInstanceId()).isNull();
            });
    }

    @Test
    void testExecuteActionWithoutTheRequiredConnectionIsRecordedAsConnectionRequired() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(connection(true));

        when(
            actionDefinitionFacade.executePerform(
                eq("slack"), eq(1), eq("sendMessage"), any(), any(), any(), any(), any(), any(), eq(Map.of()), any(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenThrow(new IllegalStateException("Connection is required"));

        assertThatThrownBy(
            () -> actionFacade.executeAction(
                "user-1", "slack", 1, "sendMessage", Map.of(), null, Environment.PRODUCTION))
                    .hasMessage("Connection is required");

        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> {
                assertThat(toolInvocationLog.getOutcome()).isEqualTo(ToolExecutionOutcome.CONNECTION_REQUIRED);
                assertThat(toolInvocationLog.getSurface()).isEqualTo(ToolExecutionSurface.EMBEDDED_API_ACTION);
                assertThat(toolInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(toolInvocationLog.getConnectionId()).isNull();
                assertThat(toolInvocationLog.getErrorType()).isNull();
            });
    }

    @Test
    void testExecuteActionOfAConnectionFreeComponentWithoutAConnectionRunsAndIsRecordedAsASuccess() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(null);

        when(
            actionDefinitionFacade.executePerform(
                eq("slack"), eq(1), eq("sendMessage"), any(), any(), any(), any(), any(), any(), eq(Map.of()), any(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn("ok");

        Object result = actionFacade.executeAction(
            "user-1", "slack", 1, "sendMessage", Map.of(), null, Environment.PRODUCTION);

        assertThat(result).isEqualTo("ok");
        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> assertThat(toolInvocationLog.getOutcome())
                .isEqualTo(ToolExecutionOutcome.SUCCESS));
    }

    @Test
    void testExecuteActionOfAComponentWithAnOptionalConnectionWithoutAConnectionRunsAndIsRecordedAsASuccess() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        stubComponentDefinition(connection(false));

        when(
            actionDefinitionFacade.executePerform(
                eq("slack"), eq(1), eq("sendMessage"), any(), any(), any(), any(), any(), any(), eq(Map.of()), any(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn("ok");

        Object result = actionFacade.executeAction(
            "user-1", "slack", 1, "sendMessage", Map.of(), null, Environment.PRODUCTION);

        assertThat(result).isEqualTo("ok");
        assertThat(getToolInvocationLogs(connectedUserId)).singleElement()
            .satisfies(toolInvocationLog -> assertThat(toolInvocationLog.getOutcome())
                .isEqualTo(ToolExecutionOutcome.SUCCESS));
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

        when(componentDefinitionService.getComponentDefinition("slack", 1))
            .thenReturn(new ComponentDefinition(modifiableComponentDefinition));
    }

    private static ModifiableConnectionDefinition connection(boolean authorizationRequired) {
        return ComponentDsl.connection()
            .authorizationRequired(authorizationRequired);
    }
}
