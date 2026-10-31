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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionIntTestConfiguration;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformWorkflowExecutionIntTestConfiguration.class)
class CustomPrincipalJobRepositoryIntTest {

    private static final Instant DAY = Instant.parse("2026-03-10T12:00:00Z");
    private static final Instant OTHER_DAY = Instant.parse("2026-03-20T12:00:00Z");

    private static final int AUTOMATION = PlatformType.AUTOMATION.ordinal();
    private static final int EMBEDDED = PlatformType.EMBEDDED.ordinal();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrincipalJobRepository principalJobRepository;

    @AfterEach
    void afterEach() {
        principalJobRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM job WHERE parent_task_execution_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");
    }

    @Test
    void testFindAllJobIdsPaginationParameterized() {
        List<Long> jobIds = saveJobs(10, PlatformType.AUTOMATION);

        Page<Long> page = principalJobRepository.findAllJobIds(
            null, null, null, null, AUTOMATION, List.of(), false, PageRequest.of(0, 3));

        assertThat(page.getContent()).containsExactly(jobIds.get(9), jobIds.get(8), jobIds.get(7));
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getTotalPages()).isEqualTo(4);
    }

    @Test
    void testFindAllJobIdsSecondPageOffset() {
        List<Long> jobIds = saveJobs(10, PlatformType.AUTOMATION);

        Page<Long> page = principalJobRepository.findAllJobIds(
            null, null, null, null, AUTOMATION, List.of(), false, PageRequest.of(1, 3));

        assertThat(page.getContent()).containsExactly(jobIds.get(6), jobIds.get(5), jobIds.get(4));
        assertThat(page.getTotalElements()).isEqualTo(10);
    }

    @Test
    void testFindAllJobIdsWithStatusFilter() {
        long completedJobId = saveJob(1L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);

        saveJob(1L, "workflow1", Job.Status.FAILED, null, null, PlatformType.AUTOMATION);
        saveJob(1L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.EMBEDDED);

        Page<Long> page = principalJobRepository.findAllJobIds(
            Job.Status.COMPLETED.ordinal(), null, null, null, AUTOMATION, List.of(), false, PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(completedJobId);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindAllJobIdsWithDateFilters() {
        long matchingJobId = saveJob(1L, "workflow1", Job.Status.COMPLETED, DAY, DAY, PlatformType.AUTOMATION);

        saveJob(1L, "workflow1", Job.Status.COMPLETED, OTHER_DAY, DAY, PlatformType.AUTOMATION);
        saveJob(1L, "workflow1", Job.Status.COMPLETED, DAY, OTHER_DAY, PlatformType.AUTOMATION);
        saveJob(1L, "workflow1", Job.Status.STARTED, DAY, null, PlatformType.AUTOMATION);

        Page<Long> page = principalJobRepository.findAllJobIds(
            null, DAY.plus(3, ChronoUnit.HOURS), DAY.minus(3, ChronoUnit.HOURS), null, AUTOMATION, List.of(), false,
            PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(matchingJobId);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindAllJobIdsWithInstanceIds() {
        long firstJobId = saveJob(1L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);
        long secondJobId = saveJob(2L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);
        long thirdJobId = saveJob(3L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);

        saveJob(4L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);

        Page<Long> page = principalJobRepository.findAllJobIds(
            null, null, null, List.of(1L, 2L, 3L), AUTOMATION, List.of(), false, PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(thirdJobId, secondJobId, firstJobId);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void testFindAllJobIdsWithWorkflowIds() {
        long firstJobId = saveJob(1L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);
        long secondJobId = saveJob(1L, "workflow2", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);

        saveJob(1L, "workflow3", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);

        Page<Long> page = principalJobRepository.findAllJobIds(
            null, null, null, null, AUTOMATION, List.of("workflow1", "workflow2"), false, PageRequest.of(0, 10));

        assertThat(page.getContent()).containsExactly(secondJobId, firstJobId);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testFindAllJobIdsOnlyRootJobsLeavesSubflowJobsOut() {
        long rootJobId = saveJob(1L, "workflow1", Job.Status.COMPLETED, null, null, PlatformType.AUTOMATION);

        long taskExecutionId = saveTaskExecution(rootJobId);

        long childJobId = saveJob(
            1L, "workflow1", Job.Status.COMPLETED, null, null, taskExecutionId, PlatformType.AUTOMATION);

        Page<Long> allJobsPage = principalJobRepository.findAllJobIds(
            null, null, null, null, AUTOMATION, List.of(), false, PageRequest.of(0, 10));
        Page<Long> rootJobsPage = principalJobRepository.findAllJobIds(
            null, null, null, null, AUTOMATION, List.of(), true, PageRequest.of(0, 10));

        assertThat(allJobsPage.getContent()).containsExactly(childJobId, rootJobId);
        assertThat(rootJobsPage.getContent()).containsExactly(rootJobId);
        assertThat(rootJobsPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindAllJobIdsEmptyResult() {
        saveJobs(3, PlatformType.AUTOMATION);

        Page<Long> page = principalJobRepository.findAllJobIds(
            null, null, null, null, EMBEDDED, List.of(), false, PageRequest.of(0, 10));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void testFindAllJobIdsAllFiltersParameterized() {
        List<Long> matchingJobIds = new ArrayList<>();

        for (int index = 0; index < 12; index++) {
            matchingJobIds.add(
                saveJob(
                    10L + (index % 2) * 10L, index % 2 == 0 ? "wf1" : "wf2", Job.Status.STARTED, DAY, DAY,
                    PlatformType.EMBEDDED));
        }

        saveJob(10L, "wf1", Job.Status.STARTED, DAY, DAY, PlatformType.AUTOMATION);
        saveJob(10L, "wf1", Job.Status.COMPLETED, DAY, DAY, PlatformType.EMBEDDED);
        saveJob(10L, "wf1", Job.Status.STARTED, OTHER_DAY, DAY, PlatformType.EMBEDDED);
        saveJob(10L, "wf1", Job.Status.STARTED, DAY, OTHER_DAY, PlatformType.EMBEDDED);
        saveJob(30L, "wf1", Job.Status.STARTED, DAY, DAY, PlatformType.EMBEDDED);
        saveJob(10L, "wf3", Job.Status.STARTED, DAY, DAY, PlatformType.EMBEDDED);

        Page<Long> page = principalJobRepository.findAllJobIds(
            Job.Status.STARTED.ordinal(), DAY, DAY, List.of(10L, 20L), EMBEDDED, List.of("wf1", "wf2"), false,
            PageRequest.of(2, 5));

        assertThat(page.getContent()).containsExactly(matchingJobIds.get(1), matchingJobIds.get(0));
        assertThat(page.getTotalElements()).isEqualTo(12);
    }

    private List<Long> saveJobs(int count, PlatformType type) {
        List<Long> jobIds = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            jobIds.add(saveJob(1L, "workflow1", Job.Status.COMPLETED, null, null, type));
        }

        return jobIds;
    }

    private long saveJob(
        long principalId, String workflowId, Job.Status status, Instant startDate, Instant endDate,
        PlatformType type) {

        return saveJob(principalId, workflowId, status, startDate, endDate, null, type);
    }

    private long saveJob(
        long principalId, String workflowId, Job.Status status, Instant startDate, Instant endDate,
        Long parentTaskExecutionId, PlatformType type) {

        Long jobId = jdbcTemplate.queryForObject(
            "INSERT INTO job (workflow_id, parent_task_execution_id, status, current_task, start_date, end_date, " +
                "priority, created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, ?, ?, 0, ?, ?, 0, now(), 'system', now(), 'system', 0) RETURNING id",
            Long.class, workflowId, parentTaskExecutionId, status.ordinal(), toTimestamp(startDate),
            toTimestamp(endDate));

        principalJobRepository.save(new PrincipalJob(principalId, jobId, type));

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

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
