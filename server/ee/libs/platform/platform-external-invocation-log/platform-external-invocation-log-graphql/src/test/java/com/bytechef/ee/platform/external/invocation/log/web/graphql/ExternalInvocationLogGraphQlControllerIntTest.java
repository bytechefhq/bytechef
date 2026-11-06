/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.dto.ExternalInvocationLogDTO;
import com.bytechef.ee.platform.external.invocation.log.facade.ExternalInvocationLogFacade;
import com.bytechef.ee.platform.external.invocation.log.web.graphql.config.ExternalInvocationLogGraphQlTestConfiguration;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ExternalInvocationLogGraphQlTestConfiguration.class,
    ExternalInvocationLogGraphQlController.class
})
@GraphQlTest(
    controllers = ExternalInvocationLogGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class ExternalInvocationLogGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ExternalInvocationLogFacade externalInvocationLogFacade;

    @Test
    void testWorkspaceExternalInvocationLogsPassesTheWorkspaceAndEnvironmentToTheFacade() {
        when(
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                eq(PlatformType.AUTOMATION), eq(7L), eq(2L), eq(ExternalInvocationSurface.MCP_AUTOMATION), isNull(),
                eq(5L),
                isNull(),
                isNull(), isNull(),
                isNull(), isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(externalInvocationLogDTO()), PageRequest.of(0, 50), 1));

        graphQlTester
            .document("""
                query {
                    workspaceExternalInvocationLogs(
                        type: AUTOMATION, workspaceId: "7", environmentId: "2", surface: "MCP_AUTOMATION",
                        mcpServerId: 5
                    ) {
                        content {
                            surface
                            kind
                            outcome
                            mcpServerId
                            workspaceId
                            workflowName
                            createdDate
                        }
                        totalElements
                    }
                }
                """)
            .execute()
            .path("workspaceExternalInvocationLogs.content[0].surface")
            .entity(String.class)
            .isEqualTo("MCP_AUTOMATION")
            .path("workspaceExternalInvocationLogs.content[0].mcpServerId")
            .entity(Long.class)
            .isEqualTo(5L)
            .path("workspaceExternalInvocationLogs.content[0].workspaceId")
            .entity(Long.class)
            .isEqualTo(7L)
            .path("workspaceExternalInvocationLogs.content[0].workflowName")
            .entity(String.class)
            .isEqualTo("Send Message")
            .path("workspaceExternalInvocationLogs.content[0].createdDate")
            .entity(Long.class)
            .isEqualTo(1_000L)
            .path("workspaceExternalInvocationLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);

        verify(externalInvocationLogFacade, never()).getExternalInvocationLogs(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testExternalInvocationLogsUsesTheTenantWideFacadeQuery() {
        when(
            externalInvocationLogFacade.getExternalInvocationLogs(
                eq(PlatformType.EMBEDDED), isNull(), isNull(), isNull(), isNull(), isNull(), eq(3L), isNull(), isNull(),
                isNull(),
                eq(0)))
                    .thenReturn(new PageImpl<>(List.of()));

        graphQlTester
            .document("""
                query {
                    externalInvocationLogs(type: EMBEDDED, connectedUserId: 3) {
                        totalElements
                    }
                }
                """)
            .execute()
            .path("externalInvocationLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(0);

        verify(externalInvocationLogFacade, never()).getWorkspaceExternalInvocationLogs(
            any(), anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void testExternalInvocationLogsPassesTheEnvironmentToTheFacade() {
        when(
            externalInvocationLogFacade.getExternalInvocationLogs(
                eq(PlatformType.AUTOMATION), eq(2L), eq(ExternalInvocationSurface.MCP_AUTOMATION), isNull(), isNull(),
                isNull(),
                isNull(), isNull(), isNull(),
                isNull(), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(externalInvocationLogDTO()), PageRequest.of(0, 50), 1));

        graphQlTester
            .document("""
                query {
                    externalInvocationLogs(type: AUTOMATION, environmentId: "2", surface: "MCP_AUTOMATION") {
                        content {
                            environment
                        }
                        totalElements
                    }
                }
                """)
            .execute()
            .path("externalInvocationLogs.content[0].environment")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("externalInvocationLogs.totalElements")
            .entity(Integer.class)
            .isEqualTo(1);
    }

    @Test
    void testExternalInvocationLogsPassesTheA2aServerToTheFacade() {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setId(2L);
        externalInvocationLog.setSurface(ExternalInvocationSurface.A2A);
        externalInvocationLog.setKind(ExternalInvocationKind.WORKFLOW);
        externalInvocationLog.setToolName("Summarize");
        externalInvocationLog.setA2aServerId(4L);
        externalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        externalInvocationLog.setDurationMs(42);

        when(
            externalInvocationLogFacade.getExternalInvocationLogs(
                eq(PlatformType.AUTOMATION), isNull(), eq(ExternalInvocationSurface.A2A), isNull(), isNull(), eq(4L),
                isNull(), isNull(), isNull(), isNull(), eq(0)))
                    .thenReturn(
                        new PageImpl<>(
                            List.of(new ExternalInvocationLogDTO(externalInvocationLog, null)), PageRequest.of(0, 50),
                            1));

        graphQlTester
            .document("""
                query {
                    externalInvocationLogs(type: AUTOMATION, surface: "A2A", a2aServerId: 4) {
                        content {
                            surface
                            a2aServerId
                            mcpServerId
                        }
                    }
                }
                """)
            .execute()
            .path("externalInvocationLogs.content[0].surface")
            .entity(String.class)
            .isEqualTo("A2A")
            .path("externalInvocationLogs.content[0].a2aServerId")
            .entity(Long.class)
            .isEqualTo(4L)
            .path("externalInvocationLogs.content[0].mcpServerId")
            .valueIsNull();
    }

    @Test
    void testControllerCarriesNoAuthorizationOfItsOwn() {
        List<Method> guardedMethods = Arrays.stream(ExternalInvocationLogGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .toList();

        assertThat(guardedMethods).isEmpty();
    }

    private static ExternalInvocationLogDTO externalInvocationLogDTO() {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setId(1L);
        externalInvocationLog.setSurface(ExternalInvocationSurface.MCP_AUTOMATION);
        externalInvocationLog.setKind(ExternalInvocationKind.WORKFLOW);
        externalInvocationLog.setJobId(11L);
        externalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        externalInvocationLog.setDurationMs(42);
        externalInvocationLog.setMcpServerId(5L);
        externalInvocationLog.setWorkspaceId(7L);
        externalInvocationLog.setEnvironment(2);
        externalInvocationLog.setCreatedDate(Instant.ofEpochMilli(1_000L));

        return new ExternalInvocationLogDTO(externalInvocationLog, "Send Message");
    }
}
