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
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
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
    private ExternalInvocationLogService externalInvocationLogService;

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
        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(externalInvocationLog.getSurface()).isEqualTo(ExternalInvocationSurface.EMBEDDED_API_ACTION);
                assertThat(externalInvocationLog.getKind()).isEqualTo(ExternalInvocationKind.COMPONENT);
                assertThat(externalInvocationLog.getToolName()).isEqualTo("sendMessage");
                assertThat(externalInvocationLog.getComponentName()).isEqualTo("slack");
                assertThat(externalInvocationLog.getComponentVersion()).isEqualTo(1);
                assertThat(externalInvocationLog.getOperationName()).isEqualTo("sendMessage");
                assertThat(externalInvocationLog.getConnectionId()).isEqualTo(7L);
                assertThat(externalInvocationLog.getEnvironment()).isEqualTo(Environment.STAGING.ordinal());
                assertThat(externalInvocationLog.getExternalUserId()).isEqualTo("user-1");
                assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationLog.getIntegrationInstanceId()).isEqualTo(integrationInstance.getId());
                assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.SUCCESS);
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

        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.ERROR);
                assertThat(externalInvocationLog.getErrorType()).isEqualTo("IllegalStateException");
                assertThat(externalInvocationLog.getErrorMessage()).isEqualTo("Channel not found");
                assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationLog.getConnectionId()).isNull();
                assertThat(externalInvocationLog.getIntegrationInstanceId()).isNull();
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

        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> {
                assertThat(externalInvocationLog.getOutcome()).isEqualTo(ExternalInvocationOutcome.CONNECTION_REQUIRED);
                assertThat(externalInvocationLog.getSurface()).isEqualTo(ExternalInvocationSurface.EMBEDDED_API_ACTION);
                assertThat(externalInvocationLog.getConnectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationLog.getConnectionId()).isNull();
                assertThat(externalInvocationLog.getErrorType()).isNull();
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
        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> assertThat(externalInvocationLog.getOutcome())
                .isEqualTo(ExternalInvocationOutcome.SUCCESS));
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
        assertThat(getExternalInvocationLogs(connectedUserId)).singleElement()
            .satisfies(externalInvocationLog -> assertThat(externalInvocationLog.getOutcome())
                .isEqualTo(ExternalInvocationOutcome.SUCCESS));
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

        when(componentDefinitionService.getComponentDefinition("slack", 1))
            .thenReturn(new ComponentDefinition(modifiableComponentDefinition));
    }

    private static ModifiableConnectionDefinition connection(boolean authorizationRequired) {
        return ComponentDsl.connection()
            .authorizationRequired(authorizationRequired);
    }
}
