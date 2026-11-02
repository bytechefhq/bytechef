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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.domain.ApprovalTaskStatusException;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskApiFacadeImpl;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.automation.task.web.graphql.config.ApprovalTaskGraphQlMethodSecurityTestConfiguration;
import com.bytechef.automation.task.web.graphql.config.ApprovalTaskGraphQlMethodSecurityTestConfiguration.TenantAdminCheck;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    ApprovalTaskApiFacadeImpl.class, ApprovalTaskGraphQlController.class,
    ApprovalTaskGraphQlMethodSecurityTestConfiguration.class
})
@GraphQlTest(
    controllers = ApprovalTaskGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class ApprovalTaskGraphQlControllerIntTest {

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

    @MockitoBean
    private ApprovalTaskService approvalTaskService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private TenantAdminCheck tenantAdminCheck;

    @BeforeEach
    void setAuthentication() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPendingApprovalsAreDeniedToANonAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        graphQlTester.document(PENDING_APPROVALS_QUERY)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).singleElement()
                .satisfies(error -> assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN)));

        verify(approvalTaskFacade, never()).getPendingApprovals(any());
    }

    @Test
    void testPendingApprovalsAreListedForAnAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
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
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
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
    void testApprovalTaskMutationsAreDeniedToANonAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        assertForbidden("mutation { deleteApprovalTask(id: \"1\") }");

        verify(approvalTaskService, never()).delete(1L);
    }

    @Test
    void testCreateApprovalTaskIsDeniedToANonAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        assertForbidden("mutation { createApprovalTask(approvalTask: {name: \"Review\"}) { id } }");

        verifyNoInteractions(approvalTaskService);
    }

    @Test
    void testUpdateApprovalTaskIsDeniedToANonAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        assertForbidden("mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Review\"}) { id } }");

        verifyNoInteractions(approvalTaskService);
    }

    @Test
    void testApprovalTaskIsDeniedToANonAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        assertForbidden("query { approvalTask(id: \"1\") { id } }");

        verifyNoInteractions(approvalTaskService);
    }

    @Test
    void testApprovalTasksAreDeniedToANonAdmin() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(false);

        assertForbidden("query { approvalTasks { id } }");

        verifyNoInteractions(approvalTaskService);
    }

    @Test
    void testUpdateApprovalTaskKeepsOmittedFields() {
        Instant dueDate = Instant.parse("2026-12-01T10:00:00Z");

        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
        when(approvalTaskService.getApprovalTask(1L)).thenReturn(
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
        when(approvalTaskService.update(any(ApprovalTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        graphQlTester.document("mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Renamed\"}) { id } }")
            .execute()
            .path("updateApprovalTask.id")
            .entity(String.class)
            .isEqualTo("1");

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).update(approvalTaskArgumentCaptor.capture());

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
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
        when(approvalTaskService.getApprovalTask(1L)).thenReturn(
            ApprovalTask.builder()
                .id(1L)
                .name("Review")
                .dueDate(Instant.parse("2026-12-01T10:00:00Z"))
                .build());
        when(approvalTaskService.update(any(ApprovalTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        graphQlTester.document(
            "mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Review\", dueDate: null}) { id } }")
            .execute()
            .path("updateApprovalTask.id")
            .entity(String.class)
            .isEqualTo("1");

        ArgumentCaptor<ApprovalTask> approvalTaskArgumentCaptor = ArgumentCaptor.forClass(ApprovalTask.class);

        verify(approvalTaskService).update(approvalTaskArgumentCaptor.capture());

        ApprovalTask updatedApprovalTask = approvalTaskArgumentCaptor.getValue();

        assertThat(updatedApprovalTask.getDueDate()).isNull();
    }

    @Test
    void testUpdateApprovalTaskRejectedStatusChangeIsABadRequest() {
        when(tenantAdminCheck.isTenantAdmin()).thenReturn(true);
        when(approvalTaskService.getApprovalTask(1L)).thenReturn(
            ApprovalTask.builder()
                .id(1L)
                .name("Review")
                .status(Status.COMPLETED)
                .build());
        when(approvalTaskService.update(any(ApprovalTask.class))).thenThrow(
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

    private void assertForbidden(String document) {
        graphQlTester.document(document)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).singleElement()
                .satisfies(error -> assertThat(error.getErrorType()).isEqualTo(ErrorType.FORBIDDEN)));
    }
}
