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

package com.bytechef.platform.workflow.execution.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO.Kind;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class WorkflowExecutionRowRepositoryIntTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final int AUTOMATION = PlatformType.AUTOMATION.ordinal();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrincipalJobRepository principalJobRepository;

    @Autowired
    private TriggerExecutionRepository triggerExecutionRepository;

    @Autowired
    private WorkflowExecutionRowRepository workflowExecutionRowRepository;

    private String firstWorkflowExecutionId;
    private String secondWorkflowExecutionId;
    private long newestJobId;
    private long olderJobId;
    private long newestTriggerExecutionId;
    private long oldestTriggerExecutionId;

    @BeforeEach
    void beforeEach() {
        firstWorkflowExecutionId = getWorkflowExecutionId("trigger-a");
        secondWorkflowExecutionId = getWorkflowExecutionId("trigger-b");

        String otherWorkflowExecutionId = getWorkflowExecutionId("trigger-c");

        newestJobId = saveJob(9L, "workflow-1", null, NOW.minus(2, ChronoUnit.HOURS));
        olderJobId = saveJob(9L, "workflow-1", null, NOW.minus(4, ChronoUnit.HOURS));

        long taskExecutionId = saveTaskExecution(newestJobId);

        saveJob(9L, "workflow-1", taskExecutionId, NOW);
        saveJob(8L, "workflow-1", null, NOW);
        saveJob(9L, "workflow-2", null, NOW);

        newestTriggerExecutionId = saveTriggerExecution(
            firstWorkflowExecutionId, TriggerExecution.Status.FAILED, NOW.minus(1, ChronoUnit.HOURS), null);
        oldestTriggerExecutionId = saveTriggerExecution(
            secondWorkflowExecutionId, TriggerExecution.Status.FAILED, NOW.minus(5, ChronoUnit.HOURS), null);

        saveTriggerExecution(firstWorkflowExecutionId, TriggerExecution.Status.FAILED, NOW, newestJobId);
        saveTriggerExecution(firstWorkflowExecutionId, TriggerExecution.Status.COMPLETED, NOW, null);
        saveTriggerExecution(otherWorkflowExecutionId, TriggerExecution.Status.FAILED, NOW, null);
    }

    @AfterEach
    void afterEach() {
        triggerExecutionRepository.deleteAll();
        principalJobRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM job WHERE parent_task_execution_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");
    }

    @Test
    void testFailedJoblessTriggersAreUnionedInAndOrderedWithJobsByStartDate() {
        Page<WorkflowExecutionRowDTO> page = workflowExecutionRowRepository.findAll(
            null, null, null, List.of(9L), AUTOMATION, List.of("workflow-1"), true,
            List.of(firstWorkflowExecutionId, secondWorkflowExecutionId), PageRequest.of(0, 20));

        assertThat(page.getContent()).containsExactly(
            new WorkflowExecutionRowDTO(Kind.TRIGGER_EXECUTION, newestTriggerExecutionId),
            new WorkflowExecutionRowDTO(Kind.JOB, newestJobId),
            new WorkflowExecutionRowDTO(Kind.JOB, olderJobId),
            new WorkflowExecutionRowDTO(Kind.TRIGGER_EXECUTION, oldestTriggerExecutionId));
        assertThat(page.getTotalElements()).isEqualTo(4);

        Page<WorkflowExecutionRowDTO> secondPage = workflowExecutionRowRepository.findAll(
            null, null, null, List.of(9L), AUTOMATION, List.of("workflow-1"), true,
            List.of(firstWorkflowExecutionId, secondWorkflowExecutionId), PageRequest.of(1, 3));

        assertThat(secondPage.getContent()).containsExactly(
            new WorkflowExecutionRowDTO(Kind.TRIGGER_EXECUTION, oldestTriggerExecutionId));
        assertThat(secondPage.getTotalElements()).isEqualTo(4);
    }

    @Test
    void testTriggersStayOutWithoutWorkflowExecutionIds() {
        Page<WorkflowExecutionRowDTO> page = workflowExecutionRowRepository.findAll(
            null, null, null, List.of(9L), AUTOMATION, List.of("workflow-1"), true, List.of(), PageRequest.of(0, 20));

        assertThat(page.getContent()).containsExactly(
            new WorkflowExecutionRowDTO(Kind.JOB, newestJobId), new WorkflowExecutionRowDTO(Kind.JOB, olderJobId));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testAnEmptyCountShortCircuitsToAnEmptyPage() {
        Page<WorkflowExecutionRowDTO> page = workflowExecutionRowRepository.findAll(
            null, null, null, List.of(7L), AUTOMATION, List.of(), true, List.of(getWorkflowExecutionId("unknown")),
            PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    private String getWorkflowExecutionId(String triggerName) {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 9L, "workflow-uuid", triggerName);

        return workflowExecutionId.toString();
    }

    private long saveJob(long principalId, String workflowId, Long parentTaskExecutionId, Instant startDate) {
        Long jobId = jdbcTemplate.queryForObject(
            "INSERT INTO job (workflow_id, parent_task_execution_id, status, current_task, start_date, priority, " +
                "created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, ?, ?, 0, ?, 0, now(), 'system', now(), 'system', 0) RETURNING id",
            Long.class, workflowId, parentTaskExecutionId, Job.Status.COMPLETED.ordinal(), Timestamp.from(startDate));

        principalJobRepository.save(new PrincipalJob(principalId, jobId, PlatformType.AUTOMATION));

        return jobId;
    }

    private long saveTaskExecution(long jobId) {
        Long taskExecutionId = jdbcTemplate.queryForObject(
            "INSERT INTO task_execution (job_id, status, progress, priority, max_retries, retry_attempts, " +
                "retry_delay, retry_delay_factor, execution_time, workflow_task, task_number, created_date, " +
                "created_by, last_modified_date, last_modified_by) " +
                "VALUES (?, 0, 0, 0, 0, 0, '1s', 2, 0, '{}', 0, now(), 'system', now(), 'system') RETURNING id",
            Long.class, jobId);

        return taskExecutionId;
    }

    private long saveTriggerExecution(
        String workflowExecutionId, TriggerExecution.Status status, Instant startDate, Long jobId) {

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .startDate(startDate)
            .status(status)
            .workflowExecutionId(WorkflowExecutionId.parse(workflowExecutionId))
            .workflowTrigger(new WorkflowTrigger(Map.of("name", "trigger", "type", "manual/v1/manual")))
            .build();

        if (jobId != null) {
            triggerExecution.setJobIds(List.of(jobId));
        }

        triggerExecution = triggerExecutionRepository.save(triggerExecution);

        return triggerExecution.getId();
    }
}
