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
import static org.mockito.Mockito.when;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.domain.ApprovalTaskStatusException;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = ApprovalTaskGraphQlController.class)
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

        graphQlTester.document("mutation { updateApprovalTask(approvalTask: {id: \"1\", name: \"Renamed\"}) { id } }")
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
