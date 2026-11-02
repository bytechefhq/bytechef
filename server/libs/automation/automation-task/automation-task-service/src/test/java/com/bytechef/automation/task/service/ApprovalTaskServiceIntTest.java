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

import com.bytechef.automation.task.config.AutomationTaskIntTestConfiguration;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.ApprovalTask.Status;
import com.bytechef.automation.task.domain.ApprovalTaskStatusException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationTaskIntTestConfiguration.class)
class ApprovalTaskServiceIntTest {

    @Autowired
    private ApprovalTaskService approvalTaskService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM approval_task");
    }

    @Test
    void testUpdateMovesAnOpenTaskToInProgress() {
        ApprovalTask approvalTask = createApprovalTask(Status.OPEN);

        approvalTaskService.update(requestedApprovalTask(approvalTask, Status.IN_PROGRESS));

        ApprovalTask storedApprovalTask = getApprovalTask(approvalTask);

        assertThat(storedApprovalTask.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(storedApprovalTask.getName()).isEqualTo("renamed");
    }

    @Test
    void testUpdateRejectsReopeningACompletedTask() {
        ApprovalTask approvalTask = createApprovalTask(Status.COMPLETED);

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(approvalTask, Status.OPEN)))
            .isInstanceOf(ApprovalTaskStatusException.class);

        assertUnchanged(approvalTask, Status.COMPLETED);
    }

    @Test
    void testUpdateRejectsReopeningAnExpiredTask() {
        ApprovalTask approvalTask = createApprovalTask(Status.EXPIRED);

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(approvalTask, Status.IN_PROGRESS)))
            .isInstanceOf(ApprovalTaskStatusException.class);

        assertUnchanged(approvalTask, Status.EXPIRED);
    }

    @Test
    void testUpdateRejectsResolvingAnOpenTask() {
        ApprovalTask approvalTask = createApprovalTask(Status.OPEN);

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(approvalTask, Status.COMPLETED)))
            .isInstanceOf(ApprovalTaskStatusException.class);

        assertUnchanged(approvalTask, Status.OPEN);
    }

    @Test
    void testUpdateWithAStaleVersionIsRejected() {
        ApprovalTask approvalTask = createApprovalTask(Status.OPEN);

        approvalTaskService.update(requestedApprovalTask(approvalTask, Status.IN_PROGRESS));

        assertThatThrownBy(() -> approvalTaskService.update(requestedApprovalTask(approvalTask, Status.OPEN)))
            .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(getApprovalTask(approvalTask).getStatus()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void testResolveCompletesAnInProgressTask() {
        ApprovalTask approvalTask = createApprovalTask(Status.IN_PROGRESS);

        approvalTaskService.resolve(requestedApprovalTask(approvalTask, Status.COMPLETED));

        assertThat(getApprovalTask(approvalTask).getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void testResolveRejectsAnAlreadyResolvedTask() {
        ApprovalTask approvalTask = createApprovalTask(Status.EXPIRED);

        assertThatThrownBy(() -> approvalTaskService.resolve(requestedApprovalTask(approvalTask, Status.COMPLETED)))
            .isInstanceOf(IllegalStateException.class);

        assertUnchanged(approvalTask, Status.EXPIRED);
    }

    @Test
    void testResolveRejectsANonTerminalStatus() {
        ApprovalTask approvalTask = createApprovalTask(Status.OPEN);

        assertThatThrownBy(() -> approvalTaskService.resolve(requestedApprovalTask(approvalTask, Status.IN_PROGRESS)))
            .isInstanceOf(IllegalArgumentException.class);

        assertUnchanged(approvalTask, Status.OPEN);
    }

    @Test
    void testGetUnresolvedApprovalTasksSkipsResolvedTasks() {
        ApprovalTask openApprovalTask = createApprovalTask(Status.OPEN);
        ApprovalTask inProgressApprovalTask = createApprovalTask(Status.IN_PROGRESS);

        createApprovalTask(Status.COMPLETED);
        createApprovalTask(Status.EXPIRED);

        List<ApprovalTask> unresolvedApprovalTasks = approvalTaskService.getUnresolvedApprovalTasks();

        assertThat(unresolvedApprovalTasks).extracting(ApprovalTask::getId)
            .containsExactlyInAnyOrder(openApprovalTask.getId(), inProgressApprovalTask.getId());
    }

    @Test
    void testFetchApprovalTaskByJobResumeId() {
        ApprovalTask approvalTask = createApprovalTask(Status.OPEN);

        assertThat(approvalTaskService.fetchApprovalTaskByJobResumeId(approvalTask.getJobResumeId()))
            .map(ApprovalTask::getId)
            .contains(approvalTask.getId());
        assertThat(approvalTaskService.fetchApprovalTaskByJobResumeId("unknown")).isEmpty();
    }

    private void assertUnchanged(ApprovalTask approvalTask, Status status) {
        ApprovalTask storedApprovalTask = getApprovalTask(approvalTask);

        assertThat(storedApprovalTask.getStatus()).isEqualTo(status);
        assertThat(storedApprovalTask.getName()).isEqualTo("name");
        assertThat(storedApprovalTask.getVersion()).isEqualTo(approvalTask.getVersion());
    }

    private ApprovalTask createApprovalTask(Status status) {
        ApprovalTask approvalTask = new ApprovalTask();

        approvalTask.setJobResumeId("jobResumeId-" + System.nanoTime());
        approvalTask.setName("name");
        approvalTask.setStatus(status);

        return approvalTaskService.create(approvalTask);
    }

    private ApprovalTask getApprovalTask(ApprovalTask approvalTask) {
        return approvalTaskService.getApprovalTask(Objects.requireNonNull(approvalTask.getId()));
    }

    private static ApprovalTask requestedApprovalTask(ApprovalTask approvalTask, Status status) {
        return ApprovalTask.builder()
            .id(approvalTask.getId())
            .name("renamed")
            .jobResumeId(approvalTask.getJobResumeId())
            .status(status)
            .priority(approvalTask.getPriority())
            .version(approvalTask.getVersion())
            .build();
    }
}
