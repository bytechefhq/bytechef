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

package com.bytechef.automation.workflow.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
abstract class AbstractLazyTaskExecutionTreeBuilderRepositoryTest {

    private static final Instant BASE_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final int ITERATION_COUNT = 120;
    private static final int RETRIED_ITERATION_INTERVAL = 7;

    private LazyTaskExecutionTreeBuilder lazyTaskExecutionTreeBuilder;
    private int sequence;
    private TaskExecutionService taskExecutionService;

    protected abstract long getJobId();

    protected abstract TaskExecution save(TaskExecution taskExecution, Instant createdDate);

    protected int getCreatedSecond(int sequence) {
        return sequence;
    }

    protected void setUp(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;

        sequence = 0;

        lazyTaskExecutionTreeBuilder = new LazyTaskExecutionTreeBuilder(
            taskExecutionService, mock(JobService.class),
            () -> (taskExecution, childJob) -> new TaskExecutionDTO(taskExecution, "t", "i", null, null, childJob));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        1, 2, 50, 200
    })
    void loopPagesConcatenateToEveryIterationOnce(int pageSize) {
        TaskExecution loopTaskExecution = saveTaskExecution(null, "loop", "loop/v1", -1);
        List<List<Long>> expectedIterations = saveLoopChildren(loopTaskExecution);

        List<List<Long>> iterations = new ArrayList<>();
        TaskExecutionChildrenPageDTO page;
        int pageNumber = 0;

        do {
            page = lazyTaskExecutionTreeBuilder.getChildrenPage(loopTaskExecution, pageNumber, pageSize);

            List<List<TaskExecutionDTO>> pageIterations = page.iterations();

            assertThat(page.numberOfElements()).isEqualTo(pageIterations.size());
            assertThat(page.numberOfElements()).isLessThanOrEqualTo(pageSize);

            iterations.addAll(toIds(pageIterations));

            pageNumber++;
        } while (pageNumber < page.totalPages());

        assertThat(page.totalElements()).isEqualTo(expectedIterations.size());
        assertThat(iterations).isEqualTo(expectedIterations);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        1, 2, 50
    })
    void iterationIndexOfEveryLoopChildPointsAtThePageAndPositionHoldingIt(int pageSize) {
        TaskExecution loopTaskExecution = saveTaskExecution(null, "loop", "loop/v1", -1);

        saveLoopChildren(loopTaskExecution);

        for (TaskExecution childTaskExecution : getChildren(loopTaskExecution)) {
            int iterationIndex = taskExecutionService.getIterationIndex(childTaskExecution);

            TaskExecutionChildrenPageDTO page = lazyTaskExecutionTreeBuilder.getChildrenPage(
                loopTaskExecution, iterationIndex / pageSize, pageSize);

            List<List<TaskExecutionDTO>> pageIterations = page.iterations();

            assertThat(toIds(pageIterations).get(iterationIndex % pageSize))
                .contains(childTaskExecution.getId());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {
        1, 2, 50
    })
    void childIndexOfEveryPlainChildPointsAtThePageAndPositionHoldingIt(int pageSize) {
        TaskExecution parallelTaskExecution = saveTaskExecution(null, "parallel", "parallel/v1", -1);

        for (int index = 0; index < ITERATION_COUNT; index++) {
            saveTaskExecution(
                parallelTaskExecution.getId(), "task", "myComponent/v1/a", -1, BASE_DATE.plusSeconds(index / 3));
        }

        for (TaskExecution childTaskExecution : getChildren(parallelTaskExecution)) {
            int childIndex = taskExecutionService.getChildIndex(childTaskExecution);

            TaskExecutionChildrenPageDTO page = lazyTaskExecutionTreeBuilder.getChildrenPage(
                parallelTaskExecution, childIndex / pageSize, pageSize);

            List<TaskExecutionDTO> children = page.children();

            TaskExecutionDTO taskExecutionDTO = children.get(childIndex % pageSize);

            assertThat(taskExecutionDTO.id()).isEqualTo(childTaskExecution.getId());
        }
    }

    @Test
    void retriedIterationStartDoesNotStartANewIteration() {
        TaskExecution loopTaskExecution = saveTaskExecution(null, "loop", "loop/v1", -1);
        List<List<Long>> expectedIterations = saveLoopChildren(loopTaskExecution);

        TaskExecutionDTO loopTaskExecutionDTO = lazyTaskExecutionTreeBuilder.getTopLevelTaskExecutions(getJobId())
            .getFirst();

        assertThat(loopTaskExecutionDTO.childCount()).isEqualTo(expectedIterations.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {
        1, 2, 50
    })
    void focusPathOfAFailureAfterARetriedIterationStartPointsAtItsIteration(int pageSize) {
        TaskExecution loopTaskExecution = saveTaskExecution(null, "loop", "loop/v1", -1);
        List<List<Long>> expectedIterations = saveLoopChildren(loopTaskExecution);

        int failedIterationIndex = expectedIterations.size() - 1;

        TaskExecution failedTaskExecution = save(
            TaskExecution.builder()
                .error(new ExecutionError("failed", List.of()))
                .jobId(getJobId())
                .parentId(loopTaskExecution.getId())
                .status(TaskExecution.Status.FAILED)
                .taskNumber(ITERATION_COUNT)
                .workflowTask(new WorkflowTask(Map.of("name", "task", "type", "myComponent/v1/a")))
                .build(),
            nextCreatedDate());

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            getJobId(), taskExecutionService.getJobTopLevelTaskExecutions(getJobId()));

        assertThat(focusPath).containsExactly(
            new FocusPathElementDTO(Objects.requireNonNull(loopTaskExecution.getId()), null, null),
            new FocusPathElementDTO(Objects.requireNonNull(failedTaskExecution.getId()), failedIterationIndex, null));

        TaskExecutionChildrenPageDTO page = lazyTaskExecutionTreeBuilder.getChildrenPage(
            loopTaskExecution, failedIterationIndex / pageSize, pageSize);

        List<List<TaskExecutionDTO>> pageIterations = page.iterations();

        assertThat(toIds(pageIterations).get(failedIterationIndex % pageSize))
            .contains(failedTaskExecution.getId());
    }

    private List<TaskExecution> getChildren(TaskExecution parentTaskExecution) {
        return taskExecutionService.getParentTaskExecutions(
            Objects.requireNonNull(parentTaskExecution.getId()), 0, Integer.MAX_VALUE);
    }

    private Instant nextCreatedDate() {
        return BASE_DATE.plusSeconds(getCreatedSecond(sequence++));
    }

    private List<List<Long>> saveLoopChildren(TaskExecution loopTaskExecution) {
        List<List<Long>> iterations = new ArrayList<>();
        List<Long> leadingIteration = new ArrayList<>();

        for (int taskNumber = 1; taskNumber <= 2; taskNumber++) {
            leadingIteration.add(
                saveTaskExecution(loopTaskExecution.getId(), "task", "myComponent/v1/a", taskNumber).getId());
        }

        iterations.add(leadingIteration);

        for (int iteration = 0; iteration < ITERATION_COUNT; iteration++) {
            List<Long> iterationIds = new ArrayList<>();

            if (iteration % RETRIED_ITERATION_INTERVAL == 3) {
                iterationIds.add(saveRetriedIterationStart(loopTaskExecution).getId());
                iterationIds.add(saveRetry(loopTaskExecution).getId());
            } else {
                iterationIds.add(saveTaskExecution(loopTaskExecution.getId(), "task", "myComponent/v1/a", 0).getId());
            }

            for (int taskNumber = 1; taskNumber <= iteration % 3; taskNumber++) {
                iterationIds.add(
                    saveTaskExecution(loopTaskExecution.getId(), "task", "myComponent/v1/a", taskNumber).getId());
            }

            iterations.add(iterationIds);
        }

        return iterations;
    }

    private TaskExecution saveRetriedIterationStart(TaskExecution loopTaskExecution) {
        return save(
            TaskExecution.builder()
                .error(new ExecutionError("failed", List.of()))
                .jobId(getJobId())
                .maxRetries(1)
                .parentId(loopTaskExecution.getId())
                .status(TaskExecution.Status.FAILED)
                .taskNumber(0)
                .workflowTask(new WorkflowTask(Map.of("name", "task", "type", "myComponent/v1/a")))
                .build(),
            nextCreatedDate());
    }

    private TaskExecution saveRetry(TaskExecution loopTaskExecution) {
        return save(
            TaskExecution.builder()
                .jobId(getJobId())
                .maxRetries(1)
                .parentId(loopTaskExecution.getId())
                .retryAttempts(1)
                .status(TaskExecution.Status.COMPLETED)
                .workflowTask(new WorkflowTask(Map.of("name", "task", "type", "myComponent/v1/a")))
                .build(),
            nextCreatedDate());
    }

    private TaskExecution saveTaskExecution(@Nullable Long parentId, String name, String type, int taskNumber) {
        return saveTaskExecution(parentId, name, type, taskNumber, nextCreatedDate());
    }

    private TaskExecution saveTaskExecution(
        @Nullable Long parentId, String name, String type, int taskNumber, Instant createdDate) {

        return save(
            TaskExecution.builder()
                .jobId(getJobId())
                .parentId(parentId)
                .status(TaskExecution.Status.COMPLETED)
                .taskNumber(taskNumber)
                .workflowTask(new WorkflowTask(Map.of("name", name, "type", type)))
                .build(),
            createdDate);
    }

    private static List<List<Long>> toIds(List<List<TaskExecutionDTO>> iterations) {
        return iterations.stream()
            .map(iteration -> iteration.stream()
                .map(TaskExecutionDTO::id)
                .toList())
            .toList();
    }
}
