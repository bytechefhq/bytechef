/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.facade;

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
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.dto.ExternalInvocationLogDTO;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import com.bytechef.platform.security.web.authentication.TestNonPlatformUserAuthentication;
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
@SpringBootTest(classes = ExternalInvocationLogFacadeIntTest.Config.class)
class ExternalInvocationLogFacadeIntTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ExternalInvocationLogFacade externalInvocationLogFacade;

    @Autowired
    private ExternalInvocationLogService externalInvocationLogService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        reset(jobService, permissionService, externalInvocationLogService, workflowService);

        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTenantQueryAdmitsTenantAdminAndDelegatesToTheTenantWideServiceQuery() {
        Page<ExternalInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(externalInvocationLogService.getExternalInvocationLogs(PlatformType.AUTOMATION, null, null, null, null,
            null, 3L,
            null,
            null, null, 0))
                .thenReturn(page);

        assertThat(
            externalInvocationLogFacade.getExternalInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, null,
                3L,
                null, null, null, 0))
                    .isEmpty();
    }

    @Test
    void testTenantQueryPassesTheEnvironmentToTheService() {
        Page<ExternalInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(
            externalInvocationLogService.getExternalInvocationLogs(
                PlatformType.AUTOMATION, 2, ExternalInvocationSurface.MCP_EMBEDDED, null, null, null, null, null, null,
                null,
                0))
                    .thenReturn(page);

        assertThat(
            externalInvocationLogFacade.getExternalInvocationLogs(
                PlatformType.AUTOMATION, 2L, ExternalInvocationSurface.MCP_EMBEDDED, null, null, null, null, null, null,
                null, 0))
                    .isEmpty();
    }

    @Test
    void testTenantQueryResolvesTheWorkflowNameOfEachInvocationFromItsJob() {
        ExternalInvocationLog workflowExternalInvocationLog = new ExternalInvocationLog();

        workflowExternalInvocationLog.setJobId(11L);

        ExternalInvocationLog purgedJobExternalInvocationLog = new ExternalInvocationLog();

        purgedJobExternalInvocationLog.setJobId(12L);

        ExternalInvocationLog componentExternalInvocationLog = new ExternalInvocationLog();

        Job job = new Job();

        job.setId(11L);
        job.setWorkflowId("workflow-1");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Send Message");

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(
            externalInvocationLogService.getExternalInvocationLogs(
                PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0))
                    .thenReturn(
                        new PageImpl<>(
                            List.of(
                                workflowExternalInvocationLog, purgedJobExternalInvocationLog,
                                componentExternalInvocationLog)));
        when(jobService.getJobs(List.of(11L, 12L))).thenReturn(List.of(job));
        when(workflowService.fetchWorkflow("workflow-1")).thenReturn(Optional.of(workflow));

        Page<ExternalInvocationLogDTO> externalInvocationLogDTOPage =
            externalInvocationLogFacade.getExternalInvocationLogs(
                PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0);

        assertThat(externalInvocationLogDTOPage.getContent())
            .extracting(ExternalInvocationLogDTO::externalInvocationLog, ExternalInvocationLogDTO::workflowName)
            .containsExactly(
                tuple(workflowExternalInvocationLog, "Send Message"), tuple(purgedJobExternalInvocationLog, null),
                tuple(componentExternalInvocationLog, null));
    }

    @Test
    void testTenantQueryDeniesUserWhoIsNotTenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(
            () -> externalInvocationLogFacade.getExternalInvocationLogs(PlatformType.AUTOMATION, null, null, null, null,
                null,
                null,
                null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService);
    }

    @Test
    void testTenantQueryDeniesConnectedUserWhileChecksAreSkipped() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        authenticate(TestNonPlatformUserAuthentication.of("connected-user"));

        assertThatThrownBy(
            () -> callSkippingChecks(
                () -> externalInvocationLogFacade.getExternalInvocationLogs(
                    PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, 0)))
                        .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService);
    }

    @Test
    void testWorkspaceQueryAdmitsWorkspaceMemberAndPassesTheWorkspaceAndEnvironmentToTheService() {
        Page<ExternalInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);
        when(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, ExternalInvocationSurface.MCP_AUTOMATION, null, 5L, null, null, null,
                null,
                null,
                1))
                    .thenReturn(page);

        assertThat(
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, ExternalInvocationSurface.MCP_AUTOMATION, null, 5L, null, null, null,
                null,
                null,
                1))
                    .isEmpty();
    }

    @Test
    void testWorkspaceQueryDeniesUserWithoutWorkflowViewOnTheWorkspace() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(
            () -> externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService);
    }

    @Test
    void testWorkspaceQueryDeniesConnectedUserWhileChecksAreSkipped() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        authenticate(TestNonPlatformUserAuthentication.of("connected-user"));

        assertThatThrownBy(
            () -> callSkippingChecks(
                () -> externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                    PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, null, 0)))
                        .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService);
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
        ExternalInvocationLogService externalInvocationLogService() {
            return mock(ExternalInvocationLogService.class);
        }

        @Bean
        ExternalInvocationLogFacade externalInvocationLogFacade(
            JobService jobService, ExternalInvocationLogService externalInvocationLogService,
            WorkflowService workflowService) {

            return new ExternalInvocationLogFacadeImpl(jobService, externalInvocationLogService, workflowService);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }
    }
}
