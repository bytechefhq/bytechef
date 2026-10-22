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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.config.WorkflowExecutionRepositoryIntTestConfiguration;
import com.bytechef.error.ExecutionError;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowExecutionRepositoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcTaskExecutionRepositoryIntTest {

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @AfterEach
    public void afterEach() {
        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    public void testFindAllIdsByParentIdOrderByCreatedDate() {
        ChildrenFixture fixture = saveChildrenFixture();

        Assertions.assertEquals(
            fixture.childIds(), taskExecutionRepository.findAllIdsByParentIdOrderByCreatedDate(fixture.parentId()));
    }

    @Test
    public void testFindAllIdsByParentIdAndTaskNumberOrderByCreatedDate() {
        ChildrenFixture fixture = saveChildrenFixture();

        List<Long> childIds = fixture.childIds();

        Assertions.assertEquals(
            List.of(childIds.get(0), childIds.get(2), childIds.get(4)),
            taskExecutionRepository.findAllIdsByParentIdAndTaskNumberOrderByCreatedDate(fixture.parentId(), 0));
    }

    @Test
    public void testFindAllByParentIdOrderByCreatedDate() {
        ChildrenFixture fixture = saveChildrenFixture();

        List<Long> childIds = fixture.childIds();

        List<TaskExecution> taskExecutions = taskExecutionRepository.findAllByParentIdOrderByCreatedDate(
            fixture.parentId(), 2, 2);

        Assertions.assertEquals(
            List.of(childIds.get(2), childIds.get(3)),
            taskExecutions.stream()
                .map(TaskExecution::getId)
                .toList());
    }

    @Test
    public void testFindAllParentIdsByParentIdIn() {
        ChildrenFixture fixture = saveChildrenFixture();

        Assertions.assertEquals(5, taskExecutionRepository.findAllParentIdsByParentIdIn(List.of(fixture.parentId()))
            .size());
        Assertions.assertEquals(
            3,
            taskExecutionRepository.findAllParentIdsByParentIdInAndTaskNumber(List.of(fixture.parentId()), 0)
                .size());
    }

    @Test
    public void testFindAllByIdIn() {
        ChildrenFixture fixture = saveChildrenFixture();

        List<Long> childIds = fixture.childIds();

        List<TaskExecution> taskExecutions = taskExecutionRepository.findAllByIdIn(
            List.of(childIds.get(1), childIds.get(3)));

        Assertions.assertEquals(
            List.of(childIds.get(1), childIds.get(3)),
            taskExecutions.stream()
                .map(TaskExecution::getId)
                .sorted()
                .toList());
    }

    @Test
    public void testEmptyInputListsReturnEmptyResults() {
        Assertions.assertTrue(taskExecutionRepository.findAllByIdIn(List.of())
            .isEmpty());
        Assertions.assertTrue(taskExecutionRepository.findAllParentIdsByParentIdIn(List.of())
            .isEmpty());
        Assertions.assertTrue(taskExecutionRepository.findAllParentIdsByParentIdInAndTaskNumber(List.of(), 0)
            .isEmpty());
    }

    @Test
    public void testFindAllByJobIdAndErrorNotNullOrderByCreatedDate() {
        saveChildrenFixture();

        long secondJobId = saveJob();

        TaskExecution failedTaskExecution =
            saveTaskExecution(secondJobId, null, 0, new ExecutionError("failed", List.of()));

        saveTaskExecution(secondJobId, null, 1, null);

        List<TaskExecution> taskExecutions =
            taskExecutionRepository.findAllByJobIdAndErrorNotNullOrderByCreatedDate(secondJobId);

        Assertions.assertEquals(
            List.of(failedTaskExecution.getId()),
            taskExecutions.stream()
                .map(TaskExecution::getId)
                .toList());
    }

    @Test
    public void testFindAllByJobIdAndParentIdIsNullOrderByCreatedDate() {
        ChildrenFixture fixture = saveChildrenFixture();

        Assertions.assertEquals(
            List.of(fixture.parentId()),
            taskExecutionRepository.findAllByJobIdAndParentIdIsNullOrderByCreatedDate(fixture.jobId())
                .stream()
                .map(TaskExecution::getId)
                .toList());
    }

    @Test
    public void testCountByJobId() {
        ChildrenFixture fixture = saveChildrenFixture();

        Assertions.assertEquals(6, taskExecutionRepository.countByJobId(fixture.jobId()));
    }

    @Test
    public void testCountByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual() {
        ChildrenFixture fixture = saveChildrenFixture();

        List<Long> childIds = fixture.childIds();

        TaskExecution secondTaskExecution = taskExecutionRepository.findById(childIds.get(2))
            .orElseThrow();

        Instant secondCreatedDate = Objects.requireNonNull(secondTaskExecution.getCreatedDate());
        long secondTaskExecutionId = Objects.requireNonNull(secondTaskExecution.getId());

        Assertions.assertEquals(
            2,
            taskExecutionRepository.countByParentIdAndTaskNumberAndCreatedDateBeforeOrEqual(
                fixture.parentId(), 0, secondCreatedDate, secondTaskExecutionId));
    }

    private ChildrenFixture saveChildrenFixture() {
        long jobId = saveJob();

        TaskExecution parentTaskExecution = saveTaskExecution(jobId, null, 0, null);

        long parentId = Objects.requireNonNull(parentTaskExecution.getId());

        List<Long> childIds = List.of(
            saveTaskExecutionId(jobId, parentId, 0), saveTaskExecutionId(jobId, parentId, 1),
            saveTaskExecutionId(jobId, parentId, 0), saveTaskExecutionId(jobId, parentId, 1),
            saveTaskExecutionId(jobId, parentId, 0));

        return new ChildrenFixture(jobId, parentId, childIds);
    }

    private long saveJob() {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        Job savedJob = jobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }

    private long saveTaskExecutionId(long jobId, long parentId, int taskNumber) {
        TaskExecution taskExecution = saveTaskExecution(jobId, parentId, taskNumber, null);

        return Objects.requireNonNull(taskExecution.getId());
    }

    private TaskExecution saveTaskExecution(Long jobId, Long parentId, int taskNumber, ExecutionError error) {
        TaskExecution taskExecution = TaskExecution.builder()
            .error(error)
            .jobId(jobId)
            .parentId(parentId)
            .taskNumber(taskNumber)
            .workflowTask(new WorkflowTask(Map.of("name", "task" + taskNumber, "type", "test/v1")))
            .build();

        return taskExecutionRepository.save(taskExecution);
    }

    private record ChildrenFixture(long jobId, long parentId, List<Long> childIds) {
    }
}
