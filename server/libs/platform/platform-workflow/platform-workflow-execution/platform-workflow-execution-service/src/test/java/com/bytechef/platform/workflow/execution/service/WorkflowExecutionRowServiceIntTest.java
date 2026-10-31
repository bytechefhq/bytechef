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

package com.bytechef.platform.workflow.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class WorkflowExecutionRowServiceIntTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrincipalJobRepository principalJobRepository;

    @Autowired
    private WorkflowExecutionRowService workflowExecutionRowService;

    @AfterEach
    void afterEach() {
        principalJobRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM job");
    }

    @Test
    void testStatusAndTypeReachTheRepositoryAsOrdinalsWithTheDefaultPageSize() {
        List<Long> failedAutomationJobIds = new ArrayList<>();

        for (int index = 0; index < PrincipalJobRepository.DEFAULT_PAGE_SIZE + 2; index++) {
            failedAutomationJobIds.add(saveJob(Status.FAILED, PlatformType.AUTOMATION, index));
        }

        saveJob(Status.COMPLETED, PlatformType.AUTOMATION, 100);
        saveJob(Status.FAILED, PlatformType.EMBEDDED, 100);

        Page<WorkflowExecutionRowDTO> firstPage = workflowExecutionRowService.getWorkflowExecutionRows(
            Status.FAILED, null, null, List.of(9L), PlatformType.AUTOMATION, List.of("workflow-1"), true,
            List.of(), 0);
        Page<WorkflowExecutionRowDTO> secondPage = workflowExecutionRowService.getWorkflowExecutionRows(
            Status.FAILED, null, null, List.of(9L), PlatformType.AUTOMATION, List.of("workflow-1"), true,
            List.of(), 1);

        assertThat(firstPage.getSize()).isEqualTo(PrincipalJobRepository.DEFAULT_PAGE_SIZE);
        assertThat(firstPage.getTotalElements()).isEqualTo(PrincipalJobRepository.DEFAULT_PAGE_SIZE + 2);
        assertThat(firstPage.getContent()).hasSize(PrincipalJobRepository.DEFAULT_PAGE_SIZE);
        assertThat(secondPage.getContent()).containsExactly(
            new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, failedAutomationJobIds.get(1)),
            new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, failedAutomationJobIds.get(0)));
    }

    @Test
    void testANullStatusIsPassedThroughAsNull() {
        long failedJobId = saveJob(Status.FAILED, PlatformType.AUTOMATION, 1);
        long completedJobId = saveJob(Status.COMPLETED, PlatformType.AUTOMATION, 2);

        saveJob(Status.COMPLETED, PlatformType.EMBEDDED, 3);

        Page<WorkflowExecutionRowDTO> page = workflowExecutionRowService.getWorkflowExecutionRows(
            null, null, null, List.of(), PlatformType.AUTOMATION, List.of(), true, List.of(), 0);

        assertThat(page.getContent()).containsExactly(
            new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, completedJobId),
            new WorkflowExecutionRowDTO(WorkflowExecutionRowDTO.Kind.JOB, failedJobId));
    }

    private long saveJob(Status status, PlatformType type, int minutesAfterNow) {
        Long jobId = jdbcTemplate.queryForObject(
            "INSERT INTO job (workflow_id, status, current_task, start_date, priority, created_date, created_by, " +
                "last_modified_date, last_modified_by, version) " +
                "VALUES ('workflow-1', ?, 0, ?, 0, now(), 'system', now(), 'system', 0) RETURNING id",
            Long.class, status.ordinal(), Timestamp.from(NOW.plus(minutesAfterNow, ChronoUnit.MINUTES)));

        principalJobRepository.save(new PrincipalJob(9L, jobId, type));

        return jobId;
    }
}
