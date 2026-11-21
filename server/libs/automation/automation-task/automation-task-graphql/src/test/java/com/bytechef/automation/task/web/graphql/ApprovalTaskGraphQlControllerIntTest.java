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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.domain.ApprovalTaskStatusException;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.automation.task.facade.ApprovalTaskFacadeImpl;
import com.bytechef.automation.task.security.ApprovalTaskJobResolver;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.automation.task.web.graphql.ApprovalTaskGraphQlController.ApprovalTaskInput;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.platform.workflow.execution.token.ApprovalTokensImpl;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
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
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ApprovalTaskFacadeImpl.class, ApprovalTaskGraphQlController.class, ApprovalTaskJobResolver.class,
    ApprovalTaskGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = ApprovalTaskGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@MockitoBean(types = {
    ApprovalTaskService.class, ApprovalTokens.class, JobService.class, PermissionService.class,
    PrincipalJobService.class, ProjectDeploymentService.class, TaskExecutionService.class, WorkflowService.class
})
class ApprovalTaskGraphQlControllerIntTest {

    @Nested
    class ControllerMapping {

        private static final String PENDING_APPROVALS_QUERY = """
            query($environmentId: Int) {
                pendingApprovals(environmentId: $environmentId) {
                    jobId
                    workflowLabel
                    formUrl
                }
            }
            """;

        @MockitoBean
        private ApprovalTaskFacade approvalTaskFacade;

        @Autowired
        private GraphQlTester graphQlTester;

        @Test
        void testPendingApprovalsAreListed() {
            when(approvalTaskFacade.getPendingApprovals(null)).thenReturn(
                List.of(
                    new PendingApproval(
                        42L, "Expense approval", "https://example.com/resume/token", Instant.now(), null)));

            graphQlTester.document(PENDING_APPROVALS_QUERY)
                .execute()
                .path("pendingApprovals[0].workflowLabel")
                .entity(String.class)
                .isEqualTo("Expense approval");
        }

        @Test
        void testPendingApprovalsPassTheEnvironmentFilter() {
            when(approvalTaskFacade.getPendingApprovals(2)).thenReturn(List.of());

            graphQlTester.document(PENDING_APPROVALS_QUERY)
                .variable("environmentId", 2)
                .execute()
                .path("pendingApprovals")
                .entityList(PendingApproval.class)
                .hasSize(0);

            verify(approvalTaskFacade).getPendingApprovals(2);
        }

        @Test
        void testCreateApprovalTaskCreatesAManualApprovalTask() {
            when(approvalTaskFacade.createManualApprovalTask(any(ApprovalTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            graphQlTester.document("mutation { createApprovalTask(approvalTask: {name: \"Review\"}) { name } }")
                .execute()
                .path("createApprovalTask.name")
                .entity(String.class)
                .isEqualTo("Review");

            verify(approvalTaskFacade, never()).createApprovalTask(any());
        }

        @Test
        void testCreateApprovalTaskKeepsTheEnvironmentItWasCreatedIn() {
            when(approvalTaskFacade.createManualApprovalTask(any(ApprovalTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            graphQlTester.document(
                "mutation { createApprovalTask(approvalTask: {name: \"Review\", environmentId: 2}) { name } }")
                .execute()
                .path("createApprovalTask.name")
                .entity(String.class)
                .isEqualTo("Review");

            ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

            verify(approvalTaskFacade).createManualApprovalTask(approvalTaskArgumentCaptor.capture());

            ApprovalTask createdApprovalTask = approvalTaskArgumentCaptor.getValue();

            assertThat(createdApprovalTask.getEnvironment()).isEqualTo(Environment.PRODUCTION);
            assertThat(createdApprovalTask.getJobResumeId()).isNull();
        }

        @Test
        void testUpdateApprovalTaskKeepsOmittedFields() {
            Instant dueDate = Instant.parse("2026-12-01T10:00:00Z");

            when(approvalTaskFacade.getApprovalTask(1L)).thenReturn(
                ApprovalTask.builder()
                    .id(1L)
                    .name("Review")
                    .description("Check the invoice")
                    .status(Status.IN_PROGRESS)
                    .priority(Priority.HIGH)
                    .assigneeId(7L)
                    .dueDate(dueDate)
                    .version(3)
                    .build());
            when(approvalTaskFacade.updateApprovalTask(any(ApprovalTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            graphQlTester
                .document("mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Renamed\"}) { id } }")
                .execute()
                .path("updateApprovalTask.id")
                .entity(String.class)
                .isEqualTo("1");

            ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

            verify(approvalTaskFacade).updateApprovalTask(approvalTaskArgumentCaptor.capture());

            ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

            assertThat(updatedApprovalTask.getName()).isEqualTo("Renamed");
            assertThat(updatedApprovalTask.getDescription()).isEqualTo("Check the invoice");
            assertThat(updatedApprovalTask.getStatus()).isEqualTo(Status.IN_PROGRESS);
            assertThat(updatedApprovalTask.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(updatedApprovalTask.getAssigneeId()).isEqualTo(7L);
            assertThat(updatedApprovalTask.getDueDate()).isEqualTo(dueDate);
            assertThat(updatedApprovalTask.getVersion()).isEqualTo(3);
        }

        @Test
        void testUpdateApprovalTaskClearsAnExplicitlyNullDueDate() {
            when(approvalTaskFacade.getApprovalTask(1L)).thenReturn(
                ApprovalTask.builder()
                    .id(1L)
                    .name("Review")
                    .dueDate(Instant.parse("2026-12-01T10:00:00Z"))
                    .build());
            when(approvalTaskFacade.updateApprovalTask(any(ApprovalTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            graphQlTester.document(
                "mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Review\", dueDate: null}) { id } }")
                .execute()
                .path("updateApprovalTask.id")
                .entity(String.class)
                .isEqualTo("1");

            ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

            verify(approvalTaskFacade).updateApprovalTask(approvalTaskArgumentCaptor.capture());

            ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

            assertThat(updatedApprovalTask.getDueDate()).isNull();
        }

        @Test
        void testUpdateApprovalTaskRejectedStatusChangeIsABadRequest() {
            when(approvalTaskFacade.getApprovalTask(1L)).thenReturn(
                ApprovalTask.builder()
                    .id(1L)
                    .name("Review")
                    .status(Status.COMPLETED)
                    .build());
            when(approvalTaskFacade.updateApprovalTask(any(ApprovalTask.class))).thenThrow(
                new ApprovalTaskStatusException("Cannot change approval task 1 to OPEN; it is already COMPLETED"));

            graphQlTester.document(
                "mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Review\", status: OPEN}) { id } }")
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).singleElement()
                    .satisfies(error -> {
                        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(error.getMessage())
                            .isEqualTo("Cannot change approval task 1 to OPEN; it is already COMPLETED");
                    }));
        }
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final long APPROVAL_TASK_ID = 3L;
        private static final String BODY_REACHED = "body reached";
        private static final long CURRENT_USER_ID = 7L;
        private static final long JOB_ID = 9L;
        private static final ApprovalTokens SIGNING_APPROVAL_TOKENS = newApprovalTokens(
            "approval-token-secret-key-0001!!");
        private static final long OTHER_USER_ID = 8L;

        @Autowired
        private ApprovalTaskGraphQlController approvalTaskGraphQlController;

        @Autowired
        private ApprovalTaskFacade approvalTaskFacade;

        @Autowired
        private ApprovalTaskService approvalTaskService;

        @Autowired
        private ApprovalTokens approvalTokens;

        @Autowired
        private PermissionService permissionService;

        @Autowired
        private PrincipalJobService principalJobService;

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("member", null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            SecurityContextHolder.setContext(securityContext);

            when(approvalTaskService.create(any(ApprovalTask.class)))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(approvalTaskService.getApprovalTask(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
            when(approvalTaskService.update(any(ApprovalTask.class)))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(approvalTokens.resolveInnerToken(anyString())).thenAnswer(
                invocation -> SIGNING_APPROVAL_TOKENS.resolveInnerToken(invocation.getArgument(0)));
            when(approvalTokens.resolveJobResumeId(anyString())).thenAnswer(
                invocation -> SIGNING_APPROVAL_TOKENS.resolveJobResumeId(invocation.getArgument(0)));
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
        void testCreateApprovalTaskForASignedApprovalTokenRequiresDeploymentEditOnItsJob(boolean granted) {
            when(permissionService.hasResourceScope(JOB_ID, "Job", "DEPLOYMENT_EDIT")).thenReturn(granted);
            when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            ApprovalTask approvalTask = ApprovalTask.builder()
                .name("Approve the invoice")
                .jobResumeId(signedApprovalToken(SIGNING_APPROVAL_TOKENS))
                .build();

            assertInvocationOutcome(() -> approvalTaskFacade.createApprovalTask(approvalTask), granted);

            verify(permissionService).isTenantAdmin();
            verify(permissionService).hasResourceScope(JOB_ID, "Job", "DEPLOYMENT_EDIT");
            verifyNoMoreInteractions(permissionService);
        }

        @ParameterizedTest(name = "tenantAdmin={0}")
        @ValueSource(booleans = {
            false, true
        })
        void testCreateApprovalTaskForAnApprovalTokenSignedWithAnotherSecretGrantsNoJobScope(boolean tenantAdmin) {
            when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

            ApprovalTask approvalTask = ApprovalTask.builder()
                .name("Approve the invoice")
                .jobResumeId(signedApprovalToken(newApprovalTokens("approval-token-secret-key-0002!!")))
                .build();

            if (tenantAdmin) {
                assertThatThrownBy(() -> approvalTaskFacade.createApprovalTask(approvalTask))
                    .isInstanceOf(IllegalArgumentException.class);
            } else {
                assertThatThrownBy(() -> approvalTaskFacade.createApprovalTask(approvalTask))
                    .isInstanceOf(AccessDeniedException.class);

                verify(permissionService).hasResourceScope(-1L, "Job", "DEPLOYMENT_EDIT");
            }

            verify(permissionService).isTenantAdmin();
            verifyNoMoreInteractions(permissionService);
            verify(principalJobService, never()).fetchJobPrincipalId(anyLong(), any());
            verify(approvalTaskService, never()).create(any(ApprovalTask.class));
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
                () -> approvalTaskFacade.updateApprovalTask(approvalTask(APPROVAL_TASK_ID, null)), expected);

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

        private static ApprovalTokens newApprovalTokens(String secret) {
            Base64.Encoder encoder = Base64.getEncoder();

            return new ApprovalTokensImpl(
                Clock.systemUTC(), encoder.encodeToString(secret.getBytes(StandardCharsets.UTF_8)), List.of(),
                Duration.ofDays(1), Duration.ofSeconds(60), false);
        }

        private static String signedApprovalToken(ApprovalTokens approvalTokens) {
            JobResumeId jobResumeId = JobResumeId.of("public", JOB_ID);

            return approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofDays(1));
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
            return new ApprovalTaskInput(id, "Approve the invoice", null, null, null, null, null, null, null);
        }
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

            ObjectProvider<MethodSecurityExpressionContributor> contributorProvider =
                beanFactory.getBeanProvider(MethodSecurityExpressionContributor.class);

            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService, contributorProvider);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            return expressionHandler;
        }
    }
}
