/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ToolInvocationLogFacadeTest {

    @Mock
    private ToolInvocationLogService toolInvocationLogService;

    @Test
    void testPreAuthorizeExpressionsArePinned() throws NoSuchMethodException {
        assertThat(
            preAuthorize(
                "getToolInvocationLogs", PlatformType.class, Long.class, ToolExecutionSurface.class,
                ToolExecutionOutcome.class, Long.class,
                Long.class, Long.class, Instant.class, Instant.class, int.class))
                    .isEqualTo("isTenantAdmin()");
        assertThat(
            preAuthorize(
                "getWorkspaceToolInvocationLogs", PlatformType.class, long.class, long.class,
                ToolExecutionSurface.class,
                ToolExecutionOutcome.class, Long.class, Long.class, Long.class, Instant.class, Instant.class,
                int.class))
                    .isEqualTo("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')");
    }

    @Test
    void testWorkspaceQueryPassesTheWorkspaceAndEnvironmentToTheService() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(
            toolInvocationLogService.getWorkspaceToolInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, ToolExecutionSurface.MCP_AUTOMATION, null, 5L, null, null, null, null,
                1))
                    .thenReturn(page);

        ToolInvocationLogFacadeImpl toolInvocationLogFacade = new ToolInvocationLogFacadeImpl(toolInvocationLogService);

        assertThat(
            toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, ToolExecutionSurface.MCP_AUTOMATION, null, 5L, null, null, null, null,
                1))
                    .isSameAs(page);

        verifyNoMoreInteractions(toolInvocationLogService);
    }

    @Test
    void testTenantQueryDelegatesToTheTenantWideServiceQuery() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(toolInvocationLogService.getToolInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, 3L, null,
            null, null, 0))
                .thenReturn(page);

        ToolInvocationLogFacadeImpl toolInvocationLogFacade = new ToolInvocationLogFacadeImpl(toolInvocationLogService);

        assertThat(toolInvocationLogFacade.getToolInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, 3L,
            null, null, null, 0))
                .isSameAs(page);
    }

    @Test
    void testTenantQueryPassesTheEnvironmentToTheService() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(
            toolInvocationLogService.getToolInvocationLogs(
                PlatformType.EMBEDDED, 2, ToolExecutionSurface.MCP_EMBEDDED, null, null, null, null, null, null, 0))
                    .thenReturn(page);

        ToolInvocationLogFacadeImpl toolInvocationLogFacade = new ToolInvocationLogFacadeImpl(toolInvocationLogService);

        assertThat(
            toolInvocationLogFacade.getToolInvocationLogs(
                PlatformType.EMBEDDED, 2L, ToolExecutionSurface.MCP_EMBEDDED, null, null, null, null, null, null, 0))
                    .isSameAs(page);

        verifyNoMoreInteractions(toolInvocationLogService);
    }

    private static String preAuthorize(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ToolInvocationLogFacadeImpl.class.getMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must carry @PreAuthorize; without it any authenticated user reads the log", methodName)
            .isNotNull();

        return preAuthorize.value();
    }
}
