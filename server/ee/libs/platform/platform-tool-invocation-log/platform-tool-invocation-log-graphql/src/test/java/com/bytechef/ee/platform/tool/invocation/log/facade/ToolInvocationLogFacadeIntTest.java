/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.tool.invocation.log.domain.ToolInvocationLog;
import com.bytechef.ee.platform.tool.invocation.log.dto.ToolInvocationLogDTO;
import com.bytechef.ee.platform.tool.invocation.log.service.ToolInvocationLogService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.TestNonPlatformUserAuthentication;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ToolInvocationLogFacadeIntTest.Config.class)
class ToolInvocationLogFacadeIntTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ToolInvocationLogFacade toolInvocationLogFacade;

    @Autowired
    private ToolInvocationLogService toolInvocationLogService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        reset(jobService, permissionService, toolInvocationLogService, workflowService);

        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTenantQueryAdmitsTenantAdminAndDelegatesToTheTenantWideServiceQuery() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(toolInvocationLogService.getToolInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, null, 3L,
            null,
            null, null, 0))
                .thenReturn(page);

        assertThat(
            toolInvocationLogFacade.getToolInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, null, 3L,
                null, null, null, 0))
                    .isEmpty();
    }

    @Test
    void testTenantQueryPassesTheEnvironmentToTheService() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(
            toolInvocationLogService.getToolInvocationLogs(
                PlatformType.AUTOMATION, 2, ToolExecutionSurface.MCP_EMBEDDED, null, null, null, null, null, null, null,
                0))
                    .thenReturn(page);

        assertThat(
            toolInvocationLogFacade.getToolInvocationLogs(
                PlatformType.AUTOMATION, 2L, ToolExecutionSurface.MCP_EMBEDDED, null, null, null, null, null, null,
                null, 0))
                    .isEmpty();
    }

    @Test
    void testTenantQueryResolvesTheWorkflowNameOfEachInvocationFromItsJob() {
        ToolInvocationLog workflowToolInvocationLog = new ToolInvocationLog();

        workflowToolInvocationLog.setJobId(11L);

        ToolInvocationLog purgedJobToolInvocationLog = new ToolInvocationLog();

        purgedJobToolInvocationLog.setJobId(12L);

        ToolInvocationLog componentToolInvocationLog = new ToolInvocationLog();

        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("workflow-1");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Send Message");

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(
            toolInvocationLogService.getToolInvocationLogs(
                PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0))
                    .thenReturn(
                        new PageImpl<>(
                            List.of(
                                workflowToolInvocationLog, purgedJobToolInvocationLog, componentToolInvocationLog)));
        when(jobService.getJobs(List.of(11L, 12L))).thenReturn(List.of(job));
        when(workflowService.fetchWorkflow("workflow-1")).thenReturn(Optional.of(workflow));

        Page<ToolInvocationLogDTO> toolInvocationLogDTOPage = toolInvocationLogFacade.getToolInvocationLogs(
            PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0);

        assertThat(toolInvocationLogDTOPage.getContent())
            .extracting(ToolInvocationLogDTO::toolInvocationLog, ToolInvocationLogDTO::workflowName)
            .containsExactly(
                tuple(workflowToolInvocationLog, "Send Message"), tuple(purgedJobToolInvocationLog, null),
                tuple(componentToolInvocationLog, null));
    }

    @Test
    void testTenantQueryDeniesUserWhoIsNotTenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(
            () -> toolInvocationLogFacade.getToolInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, null,
                null,
                null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(toolInvocationLogService);
    }

    @Test
    void testTenantQueryDeniesConnectedUserWhileChecksAreSkipped() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        authenticate(TestNonPlatformUserAuthentication.of("connected-user"));

        assertThatThrownBy(
            () -> callSkippingChecks(
                () -> toolInvocationLogFacade.getToolInvocationLogs(
                    PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0)))
                        .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(toolInvocationLogService);
    }

    @Test
    void testWorkspaceQueryAdmitsWorkspaceMemberAndPassesTheWorkspaceAndEnvironmentToTheService() {
        Page<ToolInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);
        when(
            toolInvocationLogService.getWorkspaceToolInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, ToolExecutionSurface.MCP_AUTOMATION, null, 5L, null, null, null, null,
                null,
                1))
                    .thenReturn(page);

        assertThat(
            toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, ToolExecutionSurface.MCP_AUTOMATION, null, 5L, null, null, null, null,
                null,
                1))
                    .isEmpty();
    }

    @Test
    void testWorkspaceQueryDeniesUserWithoutWorkflowViewOnTheWorkspace() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(
            () -> toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(toolInvocationLogService);
    }

    @Test
    void testWorkspaceQueryDeniesConnectedUserWhileChecksAreSkipped() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        authenticate(TestNonPlatformUserAuthentication.of("connected-user"));

        assertThatThrownBy(
            () -> callSkippingChecks(
                () -> toolInvocationLogFacade.getWorkspaceToolInvocationLogs(
                    PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, null, 0)))
                        .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(toolInvocationLogService);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static <T> T callSkippingChecks(Supplier<T> supplier) throws Throwable {
        return AutomationAuthorizationContext.callSkippingChecks(supplier::get);
    }

    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class Config {

        @Bean
        JobService jobService() {
            return mock(JobService.class);
        }

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        ToolInvocationLogService toolInvocationLogService() {
            return mock(ToolInvocationLogService.class);
        }

        @Bean
        ToolInvocationLogFacade toolInvocationLogFacade(
            JobService jobService, ToolInvocationLogService toolInvocationLogService,
            WorkflowService workflowService) {

            return new ToolInvocationLogFacadeImpl(jobService, toolInvocationLogService, workflowService);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }
    }
}
