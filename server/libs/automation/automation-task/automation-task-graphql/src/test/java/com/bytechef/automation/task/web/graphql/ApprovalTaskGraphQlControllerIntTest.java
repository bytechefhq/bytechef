/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.task.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.automation.task.web.graphql.ApprovalTaskGraphQlController.ApprovalTaskInput;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Calls the {@link ApprovalTaskGraphQlController} queries and mutations through the real Spring method-security
 * interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}, asserting each {@code @PreAuthorize} guard in both directions, each
 * {@code @PostFilter} on the tasks it keeps, and the exact checks that reach {@link PermissionService}. A task created
 * here is raised for no job and so belongs to no workspace, which leaves its creation to a tenant admin; a task is
 * changed or deleted by whoever may operate the deployment whose job raised it, and changed also by its assignee. A
 * task is read by whoever may view that deployment or by its assignee, and the tenant-wide listing shows a tenant admin
 * every task and anyone else only the tasks assigned to them.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ApprovalTaskGraphQlController.class, ApprovalTaskGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = ApprovalTaskGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/"
    })
class ApprovalTaskGraphQlControllerIntTest {

    private static final long APPROVAL_TASK_ID = 3L;
    private static final String BODY_REACHED = "body reached";
    private static final long CURRENT_USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;

    @Autowired
    private ApprovalTaskGraphQlController approvalTaskGraphQlController;

    @MockitoBean
    private ApprovalTaskService approvalTaskService;

    @MockitoBean
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);

        when(approvalTaskService.create(any(ApprovalTask.class))).thenThrow(new IllegalStateException(BODY_REACHED));
        when(approvalTaskService.getApprovalTask(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
        when(approvalTaskService.update(any(ApprovalTask.class))).thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "tenantAdmin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testCreateApprovalTaskRequiresATenantAdmin(boolean tenantAdmin) {
        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        assertInvocationOutcome(
            () -> approvalTaskGraphQlController.createApprovalTask(approvalTaskInput(null)), tenantAdmin);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "granted={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testDeleteApprovalTaskRequiresDeploymentEditOnTheTasksJob(boolean granted) {
        when(permissionService.hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT"))
            .thenReturn(granted);

        if (granted) {
            assertThat(approvalTaskGraphQlController.deleteApprovalTask(APPROVAL_TASK_ID)).isTrue();

            verify(approvalTaskService).delete(APPROVAL_TASK_ID);
        } else {
            assertThatThrownBy(() -> approvalTaskGraphQlController.deleteApprovalTask(APPROVAL_TASK_ID))
                .isInstanceOf(AccessDeniedException.class);

            verify(approvalTaskService, never()).delete(anyLong());
        }

        verify(permissionService).hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT");
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "scopeGranted={0} assignee={1}")
    @CsvSource({
        "false, false, false", "false, true, true", "true, false, true"
    })
    void testUpdateApprovalTaskRequiresDeploymentEditOnTheTasksJobOrItsAssignee(
        boolean scopeGranted, boolean assignee, boolean expected) {

        when(permissionService.hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT"))
            .thenReturn(scopeGranted);
        when(permissionService.isResourceOwner("ApprovalTask", APPROVAL_TASK_ID)).thenReturn(assignee);

        assertInvocationOutcome(
            () -> approvalTaskGraphQlController.updateApprovalTask(approvalTaskInput(APPROVAL_TASK_ID)), expected);

        verify(permissionService).hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT");

        if (scopeGranted) {
            verify(permissionService, never()).isResourceOwner("ApprovalTask", APPROVAL_TASK_ID);
        } else {
            verify(permissionService).isResourceOwner("ApprovalTask", APPROVAL_TASK_ID);
        }

        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "scopeGranted={0} assignee={1}")
    @CsvSource({
        "false, false, false", "false, true, true", "true, false, true"
    })
    void testApprovalTaskRequiresDeploymentViewOnTheTasksJobOrItsAssignee(
        boolean scopeGranted, boolean assignee, boolean expected) {

        when(permissionService.hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_VIEW"))
            .thenReturn(scopeGranted);
        when(permissionService.isResourceOwner("ApprovalTask", APPROVAL_TASK_ID)).thenReturn(assignee);

        assertInvocationOutcome(() -> approvalTaskGraphQlController.approvalTask(APPROVAL_TASK_ID), expected);

        verify(permissionService).hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_VIEW");

        if (scopeGranted) {
            verify(permissionService, never()).isResourceOwner("ApprovalTask", APPROVAL_TASK_ID);
        } else {
            verify(permissionService).isResourceOwner("ApprovalTask", APPROVAL_TASK_ID);
        }

        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "tenantAdmin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testApprovalTasksListsEveryTaskToATenantAdminAndOnlyTheirOwnTasksToOthers(boolean tenantAdmin) {
        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);
        when(permissionService.isCurrentUser(CURRENT_USER_ID)).thenReturn(true);

        ApprovalTask assignedToCaller = approvalTask(1L, CURRENT_USER_ID);
        ApprovalTask assignedToAnotherUser = approvalTask(2L, OTHER_USER_ID);
        ApprovalTask unassigned = approvalTask(3L, null);

        when(approvalTaskService.getApprovalTasks((Integer) null))
            .thenReturn(new ArrayList<>(List.of(assignedToCaller, assignedToAnotherUser, unassigned)));

        List<ApprovalTask> visibleApprovalTasks = approvalTaskGraphQlController.approvalTasks(null);

        if (tenantAdmin) {
            assertThat(visibleApprovalTasks).containsExactly(assignedToCaller, assignedToAnotherUser, unassigned);
        } else {
            assertThat(visibleApprovalTasks).containsExactly(assignedToCaller);
        }

        verify(permissionService, never()).hasResourceScope(any(), anyString(), anyString());
        verify(permissionService, never()).isResourceOwner(anyString(), anyLong());
    }

    @ParameterizedTest(name = "tenantAdmin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testApprovalTasksByIdsKeepsOnlyTheTasksTheCallerMayView(boolean tenantAdmin) {
        when(permissionService.hasResourceScope(1L, "ApprovalTask", "DEPLOYMENT_VIEW")).thenReturn(true);
        when(permissionService.hasResourceScope(2L, "ApprovalTask", "DEPLOYMENT_VIEW")).thenReturn(tenantAdmin);
        when(permissionService.hasResourceScope(3L, "ApprovalTask", "DEPLOYMENT_VIEW")).thenReturn(tenantAdmin);
        when(permissionService.isResourceOwner("ApprovalTask", 2L)).thenReturn(true);

        ApprovalTask viewableThroughItsJob = approvalTask(1L, null);
        ApprovalTask assignedToCaller = approvalTask(2L, CURRENT_USER_ID);
        ApprovalTask neither = approvalTask(3L, OTHER_USER_ID);

        List<Long> ids = List.of(1L, 2L, 3L);

        when(approvalTaskService.getApprovalTasks(ids))
            .thenReturn(new ArrayList<>(List.of(viewableThroughItsJob, assignedToCaller, neither)));

        List<ApprovalTask> visibleApprovalTasks = approvalTaskGraphQlController.approvalTasksByIds(ids);

        if (tenantAdmin) {
            assertThat(visibleApprovalTasks).containsExactly(viewableThroughItsJob, assignedToCaller, neither);
        } else {
            assertThat(visibleApprovalTasks).containsExactly(viewableThroughItsJob, assignedToCaller);
        }
    }

    private static void assertInvocationOutcome(ThrowingCallable invocation, boolean allowed) {
        if (allowed) {
            assertThatThrownBy(invocation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(invocation).isInstanceOf(AccessDeniedException.class);
        }
    }

    private static ApprovalTask approvalTask(long id, Long assigneeId) {
        return ApprovalTask.builder()
            .id(id)
            .name("Approve the invoice " + id)
            .assigneeId(assigneeId)
            .build();
    }

    private static ApprovalTaskInput approvalTaskInput(Long id) {
        return new ApprovalTaskInput(id, "Approve the invoice", null, null, null, null, null, null);
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            return expressionHandler;
        }
    }
}
