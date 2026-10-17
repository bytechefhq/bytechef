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

package com.bytechef.automation.task.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.task.domain.ApprovalTask.Priority;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskTest {

    @Test
    void testGetStatusReturnsPersistedValue() {
        assertThat(persistedApprovalTask(Status.COMPLETED.ordinal(), Priority.MEDIUM.ordinal()).getStatus())
            .isEqualTo(Status.COMPLETED);
    }

    @Test
    void testGetPriorityReturnsPersistedValue() {
        assertThat(persistedApprovalTask(Status.OPEN.ordinal(), Priority.LOW.ordinal()).getPriority())
            .isEqualTo(Priority.LOW);
    }

    @Test
    void testGetStatusRejectsUnknownOrdinal() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.values().length, Priority.MEDIUM.ordinal());

        assertThatThrownBy(approvalTask::getStatus)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("status")
            .hasMessageContaining(String.valueOf(Status.values().length));
    }

    @Test
    void testGetStatusRejectsNegativeOrdinal() {
        ApprovalTask approvalTask = persistedApprovalTask(-1, Priority.MEDIUM.ordinal());

        assertThatThrownBy(approvalTask::getStatus).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetPriorityRejectsUnknownOrdinal() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.OPEN.ordinal(), Priority.values().length);

        assertThatThrownBy(approvalTask::getPriority)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("priority")
            .hasMessageContaining(String.valueOf(Priority.values().length));
    }

    @Test
    void testGetPriorityRejectsNegativeOrdinal() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.OPEN.ordinal(), -1);

        assertThatThrownBy(approvalTask::getPriority).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCompleteResolvesAnOpenTask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.IN_PROGRESS.ordinal(), Priority.MEDIUM.ordinal());

        approvalTask.complete();

        assertThat(approvalTask.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testExpireResolvesAnOpenTask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.OPEN.ordinal(), Priority.MEDIUM.ordinal());

        approvalTask.expire();

        assertThat(approvalTask.getStatus()).isEqualTo(Status.EXPIRED);
    }

    @Test
    void testExpireRejectsACompletedTask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.COMPLETED.ordinal(), Priority.MEDIUM.ordinal());

        assertThatThrownBy(approvalTask::expire).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCompleteRejectsAnExpiredTask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.EXPIRED.ordinal(), Priority.MEDIUM.ordinal());

        assertThatThrownBy(approvalTask::complete).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testChangeProgressStatusMovesBetweenOpenAndInProgress() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.OPEN.ordinal(), Priority.MEDIUM.ordinal());

        approvalTask.changeProgressStatus(Status.IN_PROGRESS);

        assertThat(approvalTask.getStatus()).isEqualTo(Status.IN_PROGRESS);

        approvalTask.changeProgressStatus(Status.OPEN);

        assertThat(approvalTask.getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    void testChangeProgressStatusRejectsResolvingATask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.IN_PROGRESS.ordinal(), Priority.MEDIUM.ordinal());

        assertThatThrownBy(() -> approvalTask.changeProgressStatus(Status.COMPLETED))
            .isInstanceOf(IllegalStateException.class);

        assertThat(approvalTask.getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void testChangeProgressStatusRejectsReopeningAResolvedTask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.EXPIRED.ordinal(), Priority.MEDIUM.ordinal());

        assertThatThrownBy(() -> approvalTask.changeProgressStatus(Status.OPEN))
            .isInstanceOf(IllegalStateException.class);

        assertThat(approvalTask.getStatus()).isEqualTo(Status.EXPIRED);
    }

    @Test
    void testChangeProgressStatusKeepsTheCurrentStatusOfAResolvedTask() {
        ApprovalTask approvalTask = persistedApprovalTask(Status.COMPLETED.ordinal(), Priority.MEDIUM.ordinal());

        approvalTask.changeProgressStatus(Status.COMPLETED);

        assertThat(approvalTask.getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testStatusIsTerminalOnlyForCompletedAndExpired() {
        assertThat(Status.OPEN.isTerminal()).isFalse();
        assertThat(Status.IN_PROGRESS.isTerminal()).isFalse();
        assertThat(Status.COMPLETED.isTerminal()).isTrue();
        assertThat(Status.EXPIRED.isTerminal()).isTrue();
    }

    private static ApprovalTask persistedApprovalTask(int status, int priority) {
        return new ApprovalTask(1L, "name", null, "jobResumeId", 0, status, priority, null, null, 0);
    }
}
