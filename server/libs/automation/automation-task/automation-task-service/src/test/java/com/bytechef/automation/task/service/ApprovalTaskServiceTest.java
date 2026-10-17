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

package com.bytechef.automation.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskServiceTest {

    private static final long APPROVAL_TASK_ID = 1L;

    private final ApprovalTaskRepository approvalTaskRepository = mock(ApprovalTaskRepository.class);
    private final ApprovalTaskService approvalTaskService = new ApprovalTaskServiceImpl(approvalTaskRepository);

    @Test
    void testUpdateMovesAnOpenTaskToInProgress() {
        stubStoredApprovalTask(Status.OPEN);

        when(approvalTaskRepository.save(any(ApprovalTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalTask updatedApprovalTask = approvalTaskService.update(requestedApprovalTask(Status.IN_PROGRESS));

        assertThat(updatedApprovalTask.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(updatedApprovalTask.getName()).isEqualTo("renamed");
    }

    @Test
    void testUpdateRejectsReopeningACompletedTask() {
        stubStoredApprovalTask(Status.COMPLETED);

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(Status.OPEN)))
            .isInstanceOf(IllegalStateException.class);

        verify(approvalTaskRepository, never()).save(any());
    }

    @Test
    void testUpdateRejectsReopeningAnExpiredTask() {
        stubStoredApprovalTask(Status.EXPIRED);

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(Status.IN_PROGRESS)))
            .isInstanceOf(IllegalStateException.class);

        verify(approvalTaskRepository, never()).save(any());
    }

    @Test
    void testUpdateRejectsResolvingAnOpenTask() {
        stubStoredApprovalTask(Status.OPEN);

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(Status.COMPLETED)))
            .isInstanceOf(IllegalStateException.class);

        verify(approvalTaskRepository, never()).save(any());
    }

    @Test
    void testResolveCompletesAnInProgressTask() {
        stubStoredApprovalTask(Status.IN_PROGRESS);

        when(approvalTaskRepository.save(any(ApprovalTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ApprovalTask resolvedApprovalTask = approvalTaskService.resolve(requestedApprovalTask(Status.COMPLETED));

        assertThat(resolvedApprovalTask.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testResolveRejectsAnAlreadyResolvedTask() {
        stubStoredApprovalTask(Status.EXPIRED);

        assertThatThrownBy(() -> approvalTaskService.resolve(requestedApprovalTask(Status.COMPLETED)))
            .isInstanceOf(IllegalStateException.class);

        verify(approvalTaskRepository, never()).save(any());
    }

    @Test
    void testResolveRejectsANonTerminalStatus() {
        assertThatThrownBy(() -> approvalTaskService.resolve(requestedApprovalTask(Status.IN_PROGRESS)))
            .isInstanceOf(IllegalArgumentException.class);

        verify(approvalTaskRepository, never()).save(any());
    }

    private void stubStoredApprovalTask(Status status) {
        ApprovalTask storedApprovalTask = new ApprovalTask(
            APPROVAL_TASK_ID, "name", null, "jobResumeId", 0, status.ordinal(), Priority.MEDIUM.ordinal(), null, null,
            0);

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(storedApprovalTask));
    }

    private static ApprovalTask requestedApprovalTask(Status status) {
        return new ApprovalTask(
            APPROVAL_TASK_ID, "renamed", null, "jobResumeId", 0, status.ordinal(), Priority.MEDIUM.ordinal(), null,
            null, 0);
    }
}
