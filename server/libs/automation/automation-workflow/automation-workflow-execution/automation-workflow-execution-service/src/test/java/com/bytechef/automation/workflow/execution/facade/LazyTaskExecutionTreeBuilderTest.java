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

import static com.bytechef.automation.workflow.execution.facade.LazyTaskExecutionTreeBuilder.FOCUS_CANDIDATE_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class LazyTaskExecutionTreeBuilderTest {

    private static final long JOB_ID = 1L;

    private LazyTaskExecutionTreeBuilder lazyTaskExecutionTreeBuilder;
    private JobService jobService;
    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void beforeEach() {
        jobService = mock(JobService.class);
        taskExecutionService = mock(TaskExecutionService.class);

        lazyTaskExecutionTreeBuilder = new LazyTaskExecutionTreeBuilder(
            taskExecutionService, jobService,
            () -> (taskExecution, childJob) -> new TaskExecutionDTO(taskExecution, "t", "i", null, null, childJob));
    }

    @Test
    void getTopLevelTaskExecutionsSetsChildCountAndShallowChildJob() {
        TaskExecution loopTaskExecution = createTaskExecution(1L, null, "loop", "loop/v1", 0);
        TaskExecution plainTaskExecution = createTaskExecution(2L, null, "parallel", "parallel/v1", 1);
        TaskExecution subflowTaskExecution = createTaskExecution(3L, null, "subflow", "subflow/v1/run", 2);

        Job childJob = new Job(50L);
        ExecutionError childJobError = new ExecutionError("Subflow failed", List.of());

        childJob.setError(childJobError);
        childJob.setInputs(Map.of("input", "value"));
        childJob.setLabel("Child");
        childJob.setMetadata(Map.of("key", "value"));
        childJob.setParentTaskExecutionId(3L);
        childJob.setStatus(Job.Status.FAILED);
        childJob.setWorkflowId("subflow-workflow");

        when(taskExecutionService.getJobTopLevelTaskExecutions(JOB_ID))
            .thenReturn(List.of(loopTaskExecution, plainTaskExecution, subflowTaskExecution));
        when(taskExecutionService.getChildCounts(List.of(1L, 2L, 3L)))
            .thenReturn(Map.of(1L, 291L, 2L, 387L));
        when(taskExecutionService.getIterationCounts(List.of(1L, 2L, 3L)))
            .thenReturn(Map.of(1L, 97L));
        when(jobService.getChildJobs(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(childJob));

        List<TaskExecutionDTO> taskExecutionDTOs = lazyTaskExecutionTreeBuilder.getTopLevelTaskExecutions(JOB_ID);

        assertThat(taskExecutionDTOs)
            .extracting(TaskExecutionDTO::id)
            .containsExactly(1L, 2L, 3L);
        assertThat(taskExecutionDTOs)
            .extracting(TaskExecutionDTO::childCount)
            .containsExactly(97, 387, 0);

        TaskExecutionDTO firstTaskExecutionDTO = taskExecutionDTOs.get(0);

        assertThat(firstTaskExecutionDTO.childJob()).isNull();

        TaskExecutionDTO thirdTaskExecutionDTO = taskExecutionDTOs.get(2);

        JobDTO childJobDTO = thirdTaskExecutionDTO.childJob();

        assertThat(childJobDTO).isNotNull();
        assertThat(childJobDTO.id()).isEqualTo(50L);
        assertThat(childJobDTO.label()).isEqualTo("Child");
        assertThat(childJobDTO.status()).isEqualTo(Job.Status.FAILED);
        assertThat(childJobDTO.error()).isSameAs(childJobError);
        assertThat(childJobDTO.workflowId()).isEqualTo("subflow-workflow");
        assertThat(childJobDTO.taskExecutions()).isEmpty();
        assertThat(childJobDTO.inputs()).isEmpty();
        assertThat(childJobDTO.metadata()).isEmpty();
        assertThat(childJobDTO.webhooks()).isEmpty();

        verify(jobService, never()).getChildJobIds(anyLong());
        verify(jobService, never()).getJobs(argThat(ids -> true));
    }

    @Test
    void getChildJobPicksTheLatestChildJobWhenATaskHasSeveral() {
        Job firstChildJob = new Job(50L);

        firstChildJob.setParentTaskExecutionId(3L);

        Job latestChildJob = new Job(52L);

        latestChildJob.setParentTaskExecutionId(3L);

        Job secondChildJob = new Job(51L);

        secondChildJob.setParentTaskExecutionId(3L);

        when(jobService.getChildJobs(List.of(3L)))
            .thenReturn(List.of(firstChildJob, latestChildJob, secondChildJob));

        JobDTO childJobDTO = lazyTaskExecutionTreeBuilder.getChildJob(3L);

        assertThat(childJobDTO).isNotNull();
        assertThat(childJobDTO.id()).isEqualTo(52L);
    }

    @Test
    void getTopLevelTaskExecutionsCountsBranchesOfOnErrorDispatcher() {
        TaskExecution onErrorTaskExecution = createOnErrorTaskExecution(1L, null);
        TaskExecution emptyOnErrorTaskExecution = createOnErrorTaskExecution(2L, null);

        when(taskExecutionService.getJobTopLevelTaskExecutions(JOB_ID))
            .thenReturn(List.of(onErrorTaskExecution, emptyOnErrorTaskExecution));
        when(taskExecutionService.getChildCounts(List.of(1L, 2L)))
            .thenReturn(Map.of(1L, 1L));

        List<TaskExecutionDTO> taskExecutionDTOs = lazyTaskExecutionTreeBuilder.getTopLevelTaskExecutions(JOB_ID);

        assertThat(taskExecutionDTOs)
            .extracting(TaskExecutionDTO::childCount)
            .containsExactly(2, 0);
    }

    @Test
    void topLevelAndFocusPathNeverLoadEveryTaskExecutionOfTheJob() {
        TaskExecution topLevelTaskExecution = createTaskExecution(1L, null, "a", "myComponent/v1/a", 0);

        when(taskExecutionService.getJobTopLevelTaskExecutions(JOB_ID))
            .thenReturn(List.of(topLevelTaskExecution));
        when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(topLevelTaskExecution));

        lazyTaskExecutionTreeBuilder.getTopLevelTaskExecutions(JOB_ID);

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, lazyTaskExecutionTreeBuilder.getTopLevelTaskExecutionRows(JOB_ID));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(1L, null, null));

        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    void getChildrenPageReturnsPlainChildren() {
        TaskExecution parentTaskExecution = createTaskExecution(10L, null, "parallel", "parallel/v1", 0);
        TaskExecution firstChildTaskExecution = createTaskExecution(11L, 10L, "first", "myComponent/v1/a", 0);
        TaskExecution secondChildTaskExecution = createTaskExecution(12L, 10L, "second", "loop/v1", 1);

        when(taskExecutionService.getParentTaskExecutions(10L, 1, 2))
            .thenReturn(List.of(firstChildTaskExecution, secondChildTaskExecution));
        when(taskExecutionService.getChildCounts(List.of(10L)))
            .thenReturn(Map.of(10L, 5L));
        when(taskExecutionService.getIterationCounts(List.of(11L, 12L)))
            .thenReturn(Map.of(12L, 3L));

        TaskExecutionChildrenPageDTO childrenPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            parentTaskExecution, 1, 2);

        assertThat(childrenPage.children())
            .extracting(TaskExecutionDTO::id)
            .containsExactly(11L, 12L);
        assertThat(childrenPage.children())
            .extracting(TaskExecutionDTO::childCount)
            .containsExactly(0, 3);
        assertThat(childrenPage.iterations()).isEmpty();
        assertThat(childrenPage.number()).isEqualTo(1);
        assertThat(childrenPage.size()).isEqualTo(2);
        assertThat(childrenPage.numberOfElements()).isEqualTo(2);
        assertThat(childrenPage.totalElements()).isEqualTo(5L);
        assertThat(childrenPage.totalPages()).isEqualTo(3);
    }

    @Test
    void getChildrenPageKeepsIterationsWholeAcrossPages() {
        TaskExecution loopTaskExecution = createTaskExecution(10L, null, "loop", "loop/v1", 0);
        TaskExecution a0TaskExecution = createTaskExecution(11L, 10L, "first", "myComponent/v1/a", 0);
        TaskExecution a1TaskExecution = createTaskExecution(12L, 10L, "second", "myComponent/v1/a", 1);
        TaskExecution b0TaskExecution = createTaskExecution(13L, 10L, "first", "myComponent/v1/a", 0);
        TaskExecution b1TaskExecution = createTaskExecution(14L, 10L, "second", "myComponent/v1/a", 1);
        TaskExecution c0TaskExecution = createTaskExecution(15L, 10L, "first", "myComponent/v1/a", 0);

        when(taskExecutionService.getParentTaskExecutions(10L, 0, 1))
            .thenReturn(List.of(a0TaskExecution));
        when(taskExecutionService.getIterationCounts(List.of(10L)))
            .thenReturn(Map.of(10L, 3L));
        when(taskExecutionService.getParentIterationStartTaskExecutions(10L, 0L, 3))
            .thenReturn(List.of(a0TaskExecution, b0TaskExecution, c0TaskExecution));
        when(taskExecutionService.getParentTaskExecutionsBetween(10L, a0TaskExecution, c0TaskExecution))
            .thenReturn(List.of(a0TaskExecution, a1TaskExecution, b0TaskExecution, b1TaskExecution));
        when(taskExecutionService.getParentIterationStartTaskExecutions(10L, 2L, 3))
            .thenReturn(List.of(c0TaskExecution));
        when(taskExecutionService.getParentTaskExecutionsFrom(10L, c0TaskExecution))
            .thenReturn(List.of(c0TaskExecution));

        TaskExecutionChildrenPageDTO firstPage = lazyTaskExecutionTreeBuilder.getChildrenPage(loopTaskExecution, 0, 2);

        assertThat(toIds(firstPage.iterations())).isEqualTo(List.of(List.of(11L, 12L), List.of(13L, 14L)));
        assertThat(firstPage.children()).isEmpty();
        assertThat(firstPage.number()).isZero();
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.numberOfElements()).isEqualTo(2);
        assertThat(firstPage.totalElements()).isEqualTo(3L);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        TaskExecutionChildrenPageDTO secondPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            loopTaskExecution, 1, 2);

        assertThat(toIds(secondPage.iterations())).isEqualTo(List.of(List.of(15L)));
        assertThat(secondPage.number()).isEqualTo(1);
        assertThat(secondPage.numberOfElements()).isEqualTo(1);
        assertThat(secondPage.totalElements()).isEqualTo(3L);
        assertThat(secondPage.totalPages()).isEqualTo(2);

        TaskExecutionChildrenPageDTO pastLastPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            loopTaskExecution, 5, 2);

        assertThat(pastLastPage.iterations()).isEmpty();
        assertThat(pastLastPage.number()).isEqualTo(5);
        assertThat(pastLastPage.numberOfElements()).isZero();
        assertThat(pastLastPage.totalElements()).isEqualTo(3L);
        assertThat(pastLastPage.totalPages()).isEqualTo(2);
    }

    @Test
    void getChildrenPageOfLoopReadsOnlyTheRowsOfTheRequestedIterations() {
        TaskExecution loopTaskExecution = createTaskExecution(10L, null, "loop", "loop/v1", 0);
        TaskExecution firstChildTaskExecution = createTaskExecution(11L, 10L, "first", "myComponent/v1/a", 0);
        TaskExecution seventhIterationStartTaskExecution =
            createTaskExecution(611L, 10L, "first", "myComponent/v1/a", 0);
        TaskExecution seventhIterationSecondTaskExecution =
            createTaskExecution(612L, 10L, "second", "myComponent/v1/a", 1);
        TaskExecution eighthIterationStartTaskExecution =
            createTaskExecution(711L, 10L, "first", "myComponent/v1/a", 0);
        TaskExecution ninthIterationStartTaskExecution = createTaskExecution(811L, 10L, "first", "myComponent/v1/a", 0);

        when(taskExecutionService.getParentTaskExecutions(10L, 0, 1))
            .thenReturn(List.of(firstChildTaskExecution));
        when(taskExecutionService.getIterationCounts(List.of(10L)))
            .thenReturn(Map.of(10L, 100_000L));
        when(taskExecutionService.getParentIterationStartTaskExecutions(10L, 6L, 3))
            .thenReturn(List.of(seventhIterationStartTaskExecution, eighthIterationStartTaskExecution,
                ninthIterationStartTaskExecution));
        when(taskExecutionService.getParentTaskExecutionsBetween(10L, seventhIterationStartTaskExecution,
            ninthIterationStartTaskExecution))
                .thenReturn(List.of(seventhIterationStartTaskExecution, seventhIterationSecondTaskExecution,
                    eighthIterationStartTaskExecution));

        TaskExecutionChildrenPageDTO childrenPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            loopTaskExecution, 3, 2);

        assertThat(toIds(childrenPage.iterations())).isEqualTo(List.of(List.of(611L, 612L), List.of(711L)));
        assertThat(childrenPage.numberOfElements()).isEqualTo(2);
        assertThat(childrenPage.totalElements()).isEqualTo(100_000L);
        assertThat(childrenPage.totalPages()).isEqualTo(50_000);

        verify(taskExecutionService, never()).getParentTaskExecutionIds(anyLong());
        verify(taskExecutionService, never()).getParentTaskExecutions(anyLong());
        verify(taskExecutionService, never()).getJobTaskExecutions(anyLong());
    }

    @Test
    void getChildrenPageOfLoopGroupsRowsBeforeTheFirstIterationStartIntoTheirOwnIteration() {
        TaskExecution loopTaskExecution = createTaskExecution(20L, null, "loop", "loop/v1", 0);
        TaskExecution leadingTaskExecution = createTaskExecution(21L, 20L, "second", "myComponent/v1/a", 1);
        TaskExecution a0TaskExecution = createTaskExecution(22L, 20L, "first", "myComponent/v1/a", 0);
        TaskExecution a1TaskExecution = createTaskExecution(23L, 20L, "second", "myComponent/v1/a", 1);
        TaskExecution b0TaskExecution = createTaskExecution(24L, 20L, "first", "myComponent/v1/a", 0);

        when(taskExecutionService.getParentTaskExecutions(20L, 0, 1))
            .thenReturn(List.of(leadingTaskExecution));
        when(taskExecutionService.getIterationCounts(List.of(20L)))
            .thenReturn(Map.of(20L, 3L));
        when(taskExecutionService.getParentIterationStartTaskExecutions(20L, 0L, 2))
            .thenReturn(List.of(a0TaskExecution, b0TaskExecution));
        when(taskExecutionService.getParentTaskExecutionsBetween(20L, leadingTaskExecution, b0TaskExecution))
            .thenReturn(List.of(leadingTaskExecution, a0TaskExecution, a1TaskExecution));
        when(taskExecutionService.getParentIterationStartTaskExecutions(20L, 1L, 3))
            .thenReturn(List.of(b0TaskExecution));
        when(taskExecutionService.getParentTaskExecutionsFrom(20L, b0TaskExecution))
            .thenReturn(List.of(b0TaskExecution));

        TaskExecutionChildrenPageDTO firstPage = lazyTaskExecutionTreeBuilder.getChildrenPage(loopTaskExecution, 0, 2);

        assertThat(toIds(firstPage.iterations())).isEqualTo(List.of(List.of(21L), List.of(22L, 23L)));
        assertThat(firstPage.totalElements()).isEqualTo(3L);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        TaskExecutionChildrenPageDTO secondPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            loopTaskExecution, 1, 2);

        assertThat(toIds(secondPage.iterations())).isEqualTo(List.of(List.of(24L)));
        assertThat(secondPage.numberOfElements()).isEqualTo(1);
        assertThat(secondPage.totalElements()).isEqualTo(3L);
    }

    @Test
    void getChildrenPageOfLoopWithoutChildrenIsEmpty() {
        TaskExecution loopTaskExecution = createTaskExecution(10L, null, "loop", "loop/v1", 0);

        TaskExecutionChildrenPageDTO childrenPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            loopTaskExecution, 0, 2);

        assertThat(childrenPage.iterations()).isEmpty();
        assertThat(childrenPage.children()).isEmpty();
        assertThat(childrenPage.numberOfElements()).isZero();
        assertThat(childrenPage.totalElements()).isZero();
        assertThat(childrenPage.totalPages()).isZero();
    }

    @Test
    void getChildrenPageOnErrorReturnsTwoBranches() {
        TaskExecution onErrorTaskExecution = createOnErrorTaskExecution(20L, null);
        TaskExecution mainTaskExecution = createTaskExecution(21L, 20L, "mainTask", "myComponent/v1/a", 0);
        TaskExecution errorTaskExecution = createTaskExecution(22L, 20L, "errorTask", "myComponent/v1/a", 0);

        when(taskExecutionService.getParentTaskExecutionIds(20L))
            .thenReturn(List.of(21L, 22L));
        when(taskExecutionService.getTaskExecutions(List.of(21L, 22L)))
            .thenReturn(List.of(mainTaskExecution, errorTaskExecution));

        TaskExecutionChildrenPageDTO childrenPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            onErrorTaskExecution, 0, 50);

        assertThat(toIds(childrenPage.iterations())).isEqualTo(List.of(List.of(21L), List.of(22L)));
        assertThat(childrenPage.children()).isEmpty();
        assertThat(childrenPage.number()).isZero();
        assertThat(childrenPage.numberOfElements()).isEqualTo(2);
        assertThat(childrenPage.totalElements()).isEqualTo(2L);
        assertThat(childrenPage.totalPages()).isEqualTo(1);
    }

    @Test
    void countTaskExecutionsExcludesSupersededTopLevelAttempts() {
        TaskExecution firstTaskExecution = createTaskExecution(1L, null, "a", "myComponent/v1/a", 0);
        TaskExecution resumedLoopTaskExecution = createTaskExecution(3L, null, "loop", "loop/v1", 1);
        TaskExecution loopTaskExecution = createTaskExecution(4L, null, "loop", "loop/v1", 1);

        when(taskExecutionService.countJobTaskExecutions(JOB_ID, List.of(3L)))
            .thenReturn(7L);

        assertThat(
            lazyTaskExecutionTreeBuilder.countTaskExecutions(
                JOB_ID, List.of(firstTaskExecution, resumedLoopTaskExecution, loopTaskExecution)))
                    .isEqualTo(7L);
    }

    @Test
    void getFocusPathPicksFirstFailureUnderLatestAttempt() {
        TaskExecution firstTaskExecution = createTaskExecution(1L, null, "a", "myComponent/v1/a", 0);
        TaskExecution loopTaskExecution = createTaskExecution(3L, null, "loop", "loop/v1", 1);
        TaskExecution firstFailedTaskExecution = createTaskExecution(5L, 3L, "x", "myComponent/v1/a", 0);
        TaskExecution secondFailedTaskExecution = createTaskExecution(6L, 3L, "x", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(firstFailedTaskExecution, secondFailedTaskExecution));
        when(taskExecutionService.fetchTaskExecution(3L))
            .thenReturn(Optional.of(loopTaskExecution));
        when(taskExecutionService.getIterationIndex(firstFailedTaskExecution))
            .thenReturn(1);

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(firstTaskExecution, loopTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(3L, null, null),
            new FocusPathElementDTO(5L, 1, null));

        verify(taskExecutionService, never()).getChildCounts(anyList());
    }

    @Test
    void getFocusPathStopsAtFirstAcceptedFailureWithoutReadingFurtherPages() {
        TaskExecution loopTaskExecution = createTaskExecution(1L, null, "loop", "loop/v1", 0);

        List<TaskExecution> failedTaskExecutions = createFailedTaskExecutions(100L, 1L);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(failedTaskExecutions);
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(loopTaskExecution));
        when(taskExecutionService.getIterationIndex(failedTaskExecutions.getFirst()))
            .thenReturn(0);

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(loopTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(1L, null, null),
            new FocusPathElementDTO(100L, 0, null));

        verify(taskExecutionService, never())
            .getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 1, FOCUS_CANDIDATE_PAGE_SIZE);
        verify(taskExecutionService, never()).getIterationIndex(failedTaskExecutions.get(1));
    }

    @Test
    void getFocusPathReadsNextPageWhenNoFailureOnAFullPageResolves() {
        TaskExecution topLevelTaskExecution = createTaskExecution(2L, null, "a", "parallel/v1", 0);
        TaskExecution acceptedTaskExecution = createTaskExecution(900L, 2L, "y", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(
            JOB_ID, List.of(), 0, FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(createFailedTaskExecutions(100L, 77L));
        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(
            JOB_ID, List.of(), 1, FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(acceptedTaskExecution));
        when(taskExecutionService.fetchTaskExecution(77L))
            .thenReturn(Optional.empty());
        when(taskExecutionService.fetchTaskExecution(2L))
            .thenReturn(Optional.of(topLevelTaskExecution));
        when(taskExecutionService.getChildIndex(acceptedTaskExecution))
            .thenReturn(3);

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(topLevelTaskExecution));

        assertThat(focusPath).containsExactly(
            new FocusPathElementDTO(2L, null, null), new FocusPathElementDTO(900L, null, 3));

        verify(taskExecutionService).fetchTaskExecution(77L);
        verify(taskExecutionService, never())
            .getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 2, FOCUS_CANDIDATE_PAGE_SIZE);
    }

    @Test
    void getFocusPathExcludesRetriedTopLevelAttemptsFromFailureCandidates() {
        TaskExecution firstAttemptTaskExecution = createTaskExecution(1L, null, "a", "parallel/v1", 0);
        TaskExecution secondAttemptTaskExecution = createTaskExecution(2L, null, "a", "parallel/v1", 0);
        TaskExecution otherTaskExecution = createTaskExecution(3L, null, "b", "parallel/v1", 1);
        TaskExecution yTaskExecution = createTaskExecution(5L, 3L, "y", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(
            JOB_ID, List.of(1L), 0, FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(yTaskExecution));
        when(taskExecutionService.fetchTaskExecution(3L))
            .thenReturn(Optional.of(otherTaskExecution));
        when(taskExecutionService.getChildIndex(yTaskExecution))
            .thenReturn(74);

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(firstAttemptTaskExecution, secondAttemptTaskExecution, otherTaskExecution));

        assertThat(focusPath).containsExactly(
            new FocusPathElementDTO(3L, null, null), new FocusPathElementDTO(5L, null, 74));
    }

    @Test
    void getFocusPathPropagatesAnUnexpectedIllegalArgumentException() {
        TaskExecution parentTaskExecution = createTaskExecution(3L, null, "b", "parallel/v1", 0);
        TaskExecution failedTaskExecution = createTaskExecution(5L, 3L, "y", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(failedTaskExecution));
        when(taskExecutionService.fetchTaskExecution(3L))
            .thenReturn(Optional.of(parentTaskExecution));
        when(taskExecutionService.getChildIndex(failedTaskExecution))
            .thenThrow(new IllegalArgumentException("unexpected"));

        assertThatThrownBy(() -> lazyTaskExecutionTreeBuilder.getFocusPath(JOB_ID, List.of(parentTaskExecution)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("unexpected");
    }

    @Test
    void getFocusPathFallsBackToLastCreatedTaskWhenNoTaskStarted() {
        TaskExecution loopTaskExecution = createTaskExecution(1L, null, "loop", "loop/v1", 0);
        TaskExecution lastTaskExecution = createTaskExecution(9L, 1L, "x", "myComponent/v1/a", 1);

        when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(lastTaskExecution));
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(loopTaskExecution));
        when(taskExecutionService.getIterationIndex(lastTaskExecution))
            .thenReturn(2);

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(loopTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(1L, null, null),
            new FocusPathElementDTO(9L, 2, null));
    }

    @Test
    void getFocusPathFallsBackToLastStartedTaskBeforeNewerQueuedTask() {
        TaskExecution startedTaskExecution = createTaskExecution(4L, null, "a", "myComponent/v1/a", 0);
        TaskExecution queuedTaskExecution = createTaskExecution(5L, null, "b", "myComponent/v1/a", 1);

        when(taskExecutionService.fetchLastStartedJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(startedTaskExecution));
        when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(queuedTaskExecution));

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(startedTaskExecution, queuedTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(4L, null, null));
    }

    @Test
    void getFocusPathMarksOnErrorBranch() {
        TaskExecution onErrorTaskExecution = createOnErrorTaskExecution(1L, null);
        TaskExecution errorTaskExecution = createTaskExecution(3L, 1L, "errorTask", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(errorTaskExecution));
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(onErrorTaskExecution));

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(onErrorTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(1L, null, null),
            new FocusPathElementDTO(3L, 1, null));
    }

    @Test
    void getFocusPathSkipsFailureWhoseAncestorIsMissing() {
        TaskExecution orphanTaskExecution = createTaskExecution(7L, 6L, "x", "myComponent/v1/a", 0);
        TaskExecution startedTaskExecution = createTaskExecution(4L, null, "a", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(orphanTaskExecution));
        when(taskExecutionService.fetchTaskExecution(6L))
            .thenReturn(Optional.empty());
        when(taskExecutionService.fetchLastStartedJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(startedTaskExecution));

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(startedTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(4L, null, null));
    }

    @Test
    void getFocusPathSkipsFailureWhoseAncestryFormsACycle() {
        TaskExecution failedTaskExecution = createTaskExecution(7L, 6L, "x", "myComponent/v1/a", 0);
        TaskExecution cyclicParentTaskExecution = createTaskExecution(6L, 7L, "y", "myComponent/v1/a", 0);
        TaskExecution startedTaskExecution = createTaskExecution(4L, null, "a", "myComponent/v1/a", 0);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(failedTaskExecution));
        when(taskExecutionService.fetchTaskExecution(6L))
            .thenReturn(Optional.of(cyclicParentTaskExecution));
        when(taskExecutionService.fetchLastStartedJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(startedTaskExecution));

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(startedTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(4L, null, null));
    }

    @Test
    void getFocusPathFallsBackToLastStartedTaskWhenTheFailureIndexIsInvalid() {
        TaskExecution loopTaskExecution = createTaskExecution(1L, null, "loop", "loop/v1", 0);
        TaskExecution failedTaskExecution = createTaskExecution(5L, 1L, "x", "myComponent/v1/a", 0);
        TaskExecution startedTaskExecution = createTaskExecution(4L, null, "a", "myComponent/v1/a", 1);

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(JOB_ID, List.of(), 0,
            FOCUS_CANDIDATE_PAGE_SIZE))
                .thenReturn(List.of(failedTaskExecution));
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(loopTaskExecution));
        when(taskExecutionService.getIterationIndex(failedTaskExecution))
            .thenReturn(-1);
        when(taskExecutionService.fetchLastStartedJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(startedTaskExecution));

        List<FocusPathElementDTO> focusPath = lazyTaskExecutionTreeBuilder.getFocusPath(
            JOB_ID, List.of(loopTaskExecution, startedTaskExecution));

        assertThat(focusPath).containsExactly(new FocusPathElementDTO(4L, null, null));
    }

    @Test
    void getFocusPathIsEmptyWhenTheIndexOfTheLastStartedTaskCannotBeComputed() {
        TaskExecution loopTaskExecution = createTaskExecution(1L, null, "loop", "loop/v1", 0);
        TaskExecution startedTaskExecution = createTaskExecution(9L, 1L, "x", "myComponent/v1/a", 0);

        when(taskExecutionService.fetchLastStartedJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(startedTaskExecution));
        when(taskExecutionService.fetchTaskExecution(1L))
            .thenReturn(Optional.of(loopTaskExecution));
        when(taskExecutionService.getIterationIndex(startedTaskExecution))
            .thenThrow(new ArithmeticException("integer overflow"));

        assertThat(lazyTaskExecutionTreeBuilder.getFocusPath(JOB_ID, List.of(loopTaskExecution))).isEmpty();
    }

    @Test
    void getFocusPathIsEmptyWhenTheLastStartedTaskHasAMissingAncestor() {
        TaskExecution orphanTaskExecution = createTaskExecution(7L, 6L, "x", "myComponent/v1/a", 0);

        when(taskExecutionService.fetchLastStartedJobTaskExecution(JOB_ID))
            .thenReturn(Optional.of(orphanTaskExecution));
        when(taskExecutionService.fetchTaskExecution(6L))
            .thenReturn(Optional.empty());

        assertThat(lazyTaskExecutionTreeBuilder.getFocusPath(JOB_ID, List.of())).isEmpty();
    }

    @Test
    void getChildrenPageRejectsPageSizeAboveMaximum() {
        TaskExecution parentTaskExecution = createTaskExecution(30L, null, "each", "each/v1", 0);

        assertThatThrownBy(() -> lazyTaskExecutionTreeBuilder.getChildrenPage(
            parentTaskExecution, 0, LazyTaskExecutionTreeBuilder.MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lazyTaskExecutionTreeBuilder.getChildrenPage(parentTaskExecution, 0, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lazyTaskExecutionTreeBuilder.getChildrenPage(parentTaskExecution, -1, 50))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(taskExecutionService);
    }

    @Test
    void getChildrenPageOfOnErrorPastTheFirstPageIsEmpty() {
        TaskExecution onErrorTaskExecution = createOnErrorTaskExecution(1L, null);

        when(taskExecutionService.getParentTaskExecutionIds(1L))
            .thenReturn(List.of(2L, 3L));

        TaskExecutionChildrenPageDTO childrenPage = lazyTaskExecutionTreeBuilder.getChildrenPage(
            onErrorTaskExecution, 1, 50);

        assertThat(childrenPage.iterations()).isEmpty();
        assertThat(childrenPage.number()).isEqualTo(1);
        assertThat(childrenPage.totalElements()).isEqualTo(2L);
        assertThat(childrenPage.totalPages()).isEqualTo(1);

        verify(taskExecutionService, never()).getTaskExecutions(anyList());
    }

    @Test
    void getFocusPathIsEmptyWithoutTaskExecutions() {
        when(taskExecutionService.fetchLastJobTaskExecution(JOB_ID))
            .thenReturn(Optional.empty());

        assertThat(lazyTaskExecutionTreeBuilder.getFocusPath(JOB_ID, List.of())).isEmpty();
    }

    private static TaskExecution createOnErrorTaskExecution(long id, @Nullable Long parentId) {
        WorkflowTask workflowTask = new WorkflowTask(
            Map.of(
                "name", "onError",
                "type", "on-error/v1",
                "parameters", Map.of(
                    "main-branch", List.of(Map.of("name", "mainTask", "type", "myComponent/v1/a")),
                    "on-error-branch", List.of(Map.of("name", "errorTask", "type", "myComponent/v1/a")))));

        return TaskExecution.builder()
            .id(id)
            .jobId(JOB_ID)
            .parentId(parentId)
            .workflowTask(workflowTask)
            .build();
    }

    private static TaskExecution createTaskExecution(
        long id, @Nullable Long parentId, String name, String type, int taskNumber) {

        return TaskExecution.builder()
            .id(id)
            .jobId(JOB_ID)
            .parentId(parentId)
            .taskNumber(taskNumber)
            .workflowTask(new WorkflowTask(Map.of("name", name, "type", type)))
            .build();
    }

    private static List<TaskExecution> createFailedTaskExecutions(long firstId, long parentId) {
        List<TaskExecution> failedTaskExecutions = new ArrayList<>();

        for (long id = firstId; id < firstId + FOCUS_CANDIDATE_PAGE_SIZE; id++) {
            failedTaskExecutions.add(createTaskExecution(id, parentId, "x", "myComponent/v1/a", 0));
        }

        return failedTaskExecutions;
    }

    private static List<List<Long>> toIds(List<List<TaskExecutionDTO>> iterations) {
        return iterations.stream()
            .map(iteration -> iteration.stream()
                .map(TaskExecutionDTO::id)
                .toList())
            .toList();
    }
}
