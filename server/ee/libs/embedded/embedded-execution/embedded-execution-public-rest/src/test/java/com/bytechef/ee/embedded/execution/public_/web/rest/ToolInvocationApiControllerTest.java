/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationKindModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationOutcomeModel;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ToolInvocationSurfaceModel;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolInvocationApiControllerTest {

    private static final long CONNECTED_USER_ID = 500L;
    private static final Instant CREATED_DATE = Instant.parse("2026-07-18T10:30:00Z");
    private static final Instant END_DATE = Instant.parse("2026-07-18T11:00:00Z");
    private static final String EXTERNAL_USER_ID = "externalUser1";
    private static final Instant START_DATE = Instant.parse("2026-07-18T10:00:00Z");

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final ToolInvocationLogService toolInvocationLogService = mock(ToolInvocationLogService.class);

    private ToolInvocationApiController toolInvocationApiController;

    @BeforeEach
    void beforeEach() {
        toolInvocationApiController = new ToolInvocationApiController(
            connectedUserService, environmentService, toolInvocationLogService);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, "n/a"));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetToolInvocationsPageScopesToConnectedUserAndMapsModels() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
        when(environmentService.getEnvironment("PRODUCTION")).thenReturn(Environment.PRODUCTION);
        when(connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION)).thenReturn(connectedUser);
        when(
            toolInvocationLogService.getToolInvocationLogs(
                eq(ToolExecutionSurface.EMBEDDED_API_ACTION), eq(ToolExecutionOutcome.SUCCESS), isNull(),
                eq(CONNECTED_USER_ID), isNull(), eq(START_DATE), eq(END_DATE), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(createToolInvocationLog())));

        ResponseEntity<Page> response = toolInvocationApiController.getToolInvocationsPage(
            EXTERNAL_USER_ID, EnvironmentModel.PRODUCTION, ToolInvocationSurfaceModel.EMBEDDED_API_ACTION,
            ToolInvocationOutcomeModel.SUCCESS, START_DATE.atOffset(ZoneOffset.UTC), END_DATE.atOffset(ZoneOffset.UTC),
            0);

        Page<?> page = response.getBody();

        assertThat(page).isNotNull();
        assertThat(page.getContent()).hasSize(1);

        ToolInvocationModel toolInvocationModel = (ToolInvocationModel) page.getContent()
            .getFirst();

        assertThat(toolInvocationModel.getId()).isEqualTo(1L);
        assertThat(toolInvocationModel.getSurface()).isEqualTo(ToolInvocationSurfaceModel.EMBEDDED_API_ACTION);
        assertThat(toolInvocationModel.getKind()).isEqualTo(ToolInvocationKindModel.COMPONENT);
        assertThat(toolInvocationModel.getComponentName()).isEqualTo("slack");
        assertThat(toolInvocationModel.getOperationName()).isEqualTo("sendMessage");
        assertThat(toolInvocationModel.getOutcome()).isEqualTo(ToolInvocationOutcomeModel.SUCCESS);
        assertThat(toolInvocationModel.getDurationMs()).isEqualTo(42);
        assertThat(toolInvocationModel.getCreatedDate()).isEqualTo(CREATED_DATE.atOffset(ZoneOffset.UTC));
    }

    @Test
    void testGetToolInvocationsPageDefaultsToProductionEnvironment() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION)).thenReturn(connectedUser);
        when(
            toolInvocationLogService.getToolInvocationLogs(
                isNull(), isNull(), isNull(), eq(CONNECTED_USER_ID), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<Page> response = toolInvocationApiController.getToolInvocationsPage(
            EXTERNAL_USER_ID, null, null, null, null, null, null);

        assertThat(response.getBody()).isNotNull();

        verify(toolInvocationLogService).getToolInvocationLogs(
            isNull(), isNull(), isNull(), eq(CONNECTED_USER_ID), isNull(), isNull(), isNull(), eq(0));
    }

    @Test
    void testGetToolInvocationsPageDeniesMismatchedExternalUserId() {
        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(
                () -> toolInvocationApiController.getToolInvocationsPage(
                    "otherUser", null, null, null, null, null, 0));
    }

    private ToolInvocationLog createToolInvocationLog() {
        ToolInvocationLog toolInvocationLog = new ToolInvocationLog();

        toolInvocationLog.setId(1L);
        toolInvocationLog.setSurface(ToolExecutionSurface.EMBEDDED_API_ACTION);
        toolInvocationLog.setKind(ToolExecutionKind.COMPONENT);
        toolInvocationLog.setComponentName("slack");
        toolInvocationLog.setComponentVersion(1);
        toolInvocationLog.setOperationName("sendMessage");
        toolInvocationLog.setOutcome(ToolExecutionOutcome.SUCCESS);
        toolInvocationLog.setDurationMs(42);
        toolInvocationLog.setCreatedDate(CREATED_DATE);

        return toolInvocationLog;
    }
}
