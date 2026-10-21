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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.bytechef.ee.platform.external.invocation.log.security.ExternalInvocationLogAppInstanceAccess;
import com.bytechef.ee.platform.external.invocation.log.security.ViewableAppInstances;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.security.web.authentication.TestNonPlatformUserAuthentication;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
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
    private ExternalInvocationLogAppInstanceAccess externalInvocationLogAppInstanceAccess;

    @Autowired
    private JobService jobService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ExternalInvocationLogFacade externalInvocationLogFacade;

    @Autowired
    private ExternalInvocationLogService externalInvocationLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        reset(
            externalInvocationLogAppInstanceAccess, jobService, permissionService, externalInvocationLogService,
            userService, workflowService);

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
            null,
            null, null, 0))
                .thenReturn(page);

        assertThat(
            externalInvocationLogFacade.getExternalInvocationLogs(PlatformType.AUTOMATION, null, null, null, null, null,
                3L,
                null, null, null, null, 0))
                    .isEmpty();
    }

    @Test
    void testTenantQueryPassesTheEnvironmentToTheService() {
        Page<ExternalInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(
            externalInvocationLogService.getExternalInvocationLogs(
                PlatformType.AUTOMATION, 2, ExternalInvocationSource.MCP_SERVER, null, null, null, null, null, null,
                null,
                null,
                0))
                    .thenReturn(page);

        assertThat(
            externalInvocationLogFacade.getExternalInvocationLogs(
                PlatformType.AUTOMATION, 2L, ExternalInvocationSource.MCP_SERVER, null, null, null, null, null, null,
                null,
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
                PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, null, 0))
                    .thenReturn(
                        new PageImpl<>(
                            List.of(
                                workflowExternalInvocationLog, purgedJobExternalInvocationLog,
                                componentExternalInvocationLog)));
        when(jobService.getJobs(List.of(11L, 12L))).thenReturn(List.of(job));
        when(workflowService.fetchWorkflow("workflow-1")).thenReturn(Optional.of(workflow));

        Page<ExternalInvocationLogDTO> externalInvocationLogDTOPage =
            externalInvocationLogFacade.getExternalInvocationLogs(
                PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, null, 0);

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
                null, null, null, null, 0))
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
                    PlatformType.AUTOMATION, null, null, null, null, null, null, null, null, null, null, 0)))
                        .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService);
    }

    @Test
    void testWorkspaceQueryAdmitsWorkspaceMemberAndPassesTheWorkspaceAndEnvironmentToTheService() {
        Page<ExternalInvocationLog> page = new PageImpl<>(List.of());

        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);
        when(externalInvocationLogAppInstanceAccess.getViewableAppInstances(7L, 2))
            .thenReturn(ViewableAppInstances.only(Set.of(50L)));
        when(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, ExternalInvocationSource.MCP_SERVER, null, 5L, null, null, null,
                9L, 50L, ViewableAppInstances.only(Set.of(50L)), null, null, 1))
                    .thenReturn(page);

        assertThat(
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, ExternalInvocationSource.MCP_SERVER, null, 5L, null, null, null,
                9L, 50L,
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
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, 50L, null, null, 0))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService, userService);
    }

    @Test
    void testWorkspaceQueryDeniesConnectedUserWhileChecksAreSkipped() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        authenticate(TestNonPlatformUserAuthentication.of("connected-user"));

        assertThatThrownBy(
            () -> callSkippingChecks(
                () -> externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                    PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, 50L, null, null, 0)))
                        .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(externalInvocationLogService);
    }

    @Test
    void testWorkspaceQueryResolvesTheEndUserOfEachInvocationInOneLookup() {
        ExternalInvocationLog firstUserExternalInvocationLog = appExternalInvocationLog(21L);
        ExternalInvocationLog secondUserExternalInvocationLog = appExternalInvocationLog(22L);
        ExternalInvocationLog repeatedUserExternalInvocationLog = appExternalInvocationLog(21L);
        ExternalInvocationLog deletedUserExternalInvocationLog = appExternalInvocationLog(23L);
        ExternalInvocationLog keyOnlyExternalInvocationLog = appExternalInvocationLog(null);

        User firstUser = new User();

        firstUser.setId(21L);
        firstUser.setEmail("ada@example.com");
        firstUser.setFirstName("Ada");
        firstUser.setLastName("Lovelace");
        firstUser.setLogin("ada");

        User secondUser = new User();

        secondUser.setId(22L);
        secondUser.setEmail("grace@example.com");
        secondUser.setLogin("grace");

        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);
        when(externalInvocationLogAppInstanceAccess.getViewableAppInstances(7L, 2))
            .thenReturn(ViewableAppInstances.only(Set.of()));
        when(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, 9L, null,
                ViewableAppInstances.only(Set.of()), null, null,
                0))
                    .thenReturn(
                        new PageImpl<>(
                            List.of(
                                firstUserExternalInvocationLog, secondUserExternalInvocationLog,
                                repeatedUserExternalInvocationLog, deletedUserExternalInvocationLog,
                                keyOnlyExternalInvocationLog)));
        when(userService.getUsers(List.of(21L, 22L, 23L))).thenReturn(List.of(firstUser, secondUser));

        Page<ExternalInvocationLogDTO> externalInvocationLogDTOPage =
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, 9L, null, null, null, 0);

        assertThat(externalInvocationLogDTOPage.getContent())
            .extracting(ExternalInvocationLogDTO::externalInvocationLog, ExternalInvocationLogDTO::userName)
            .containsExactly(
                tuple(firstUserExternalInvocationLog, "Ada Lovelace"),
                tuple(secondUserExternalInvocationLog, "grace@example.com"),
                tuple(repeatedUserExternalInvocationLog, "Ada Lovelace"),
                tuple(deletedUserExternalInvocationLog, null),
                tuple(keyOnlyExternalInvocationLog, null));

        verify(userService, times(1)).getUsers(anyCollection());
    }

    @Test
    void testWorkspaceQueryWithoutEndUsersSkipsTheUserLookup() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);
        when(externalInvocationLogAppInstanceAccess.getViewableAppInstances(7L, 2))
            .thenReturn(ViewableAppInstances.only(Set.of()));
        when(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, 9L, null,
                ViewableAppInstances.only(Set.of()), null, null,
                0))
                    .thenReturn(new PageImpl<>(List.of(appExternalInvocationLog(null))));

        Page<ExternalInvocationLogDTO> externalInvocationLogDTOPage =
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, 9L, null, null, null, 0);

        assertThat(externalInvocationLogDTOPage.getContent())
            .extracting(ExternalInvocationLogDTO::userName)
            .containsExactly((String) null);

        verifyNoInteractions(userService);
    }

    @Test
    void testWorkspaceQueryPassesAnUnrestrictedAnswerToTheService() {
        when(permissionService.hasResourceScope(7L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);
        when(externalInvocationLogAppInstanceAccess.getViewableAppInstances(7L, 2))
            .thenReturn(ViewableAppInstances.all());
        when(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, null, null,
                ViewableAppInstances.all(), null, null, 0))
                    .thenReturn(new PageImpl<>(List.of()));

        assertThat(
            externalInvocationLogFacade.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, null, null, null, 0))
                    .isEmpty();

        verify(externalInvocationLogService).getWorkspaceExternalInvocationLogs(
            PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, null, null,
            ViewableAppInstances.all(), null, null, 0);
    }

    @Test
    void testWorkspaceQueryWithoutAnAppInstanceAccessHidesEveryAppInstanceRow() {
        ExternalInvocationLogFacade externalInvocationLogFacadeWithoutApps = new ExternalInvocationLogFacadeImpl(
            emptyProvider(ExternalInvocationLogAppInstanceAccess.class), jobService, externalInvocationLogService,
            emptyProvider(UserService.class), workflowService);

        when(
            externalInvocationLogService.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, null, null,
                ViewableAppInstances.only(Set.of()), null, null,
                0))
                    .thenReturn(new PageImpl<>(List.of()));

        assertThat(
            externalInvocationLogFacadeWithoutApps.getWorkspaceExternalInvocationLogs(
                PlatformType.AUTOMATION, 7L, 2L, null, null, null, null, null, null, null, null, null, null, 0))
                    .isEmpty();

        verify(externalInvocationLogService).getWorkspaceExternalInvocationLogs(
            PlatformType.AUTOMATION, 7L, 2, null, null, null, null, null, null, null, null,
            ViewableAppInstances.only(Set.of()), null, null, 0);
    }

    private static <T> ObjectProvider<T> emptyProvider(Class<T> type) {
        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory();

        return staticListableBeanFactory.getBeanProvider(type);
    }

    private static ExternalInvocationLog appExternalInvocationLog(Long userId) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setAppId(9L);
        externalInvocationLog.setUserId(userId);

        return externalInvocationLog;
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
        ExternalInvocationLogAppInstanceAccess externalInvocationLogAppInstanceAccess() {
            return mock(ExternalInvocationLogAppInstanceAccess.class);
        }

        @Bean
        ExternalInvocationLogFacade externalInvocationLogFacade(
            ObjectProvider<ExternalInvocationLogAppInstanceAccess> externalInvocationLogAppInstanceAccessProvider,
            JobService jobService, ExternalInvocationLogService externalInvocationLogService,
            ObjectProvider<UserService> userServiceProvider, WorkflowService workflowService) {

            return new ExternalInvocationLogFacadeImpl(
                externalInvocationLogAppInstanceAccessProvider, jobService, externalInvocationLogService,
                userServiceProvider, workflowService);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        WorkflowService workflowService() {
            return mock(WorkflowService.class);
        }
    }
}
