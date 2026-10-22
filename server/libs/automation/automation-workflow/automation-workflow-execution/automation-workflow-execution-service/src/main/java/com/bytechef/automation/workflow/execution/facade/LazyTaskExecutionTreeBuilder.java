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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
class LazyTaskExecutionTreeBuilder {

    static final int FOCUS_CANDIDATE_PAGE_SIZE = 100;
    static final int MAX_PAGE_SIZE = 200;

    private static final Logger log = LoggerFactory.getLogger(LazyTaskExecutionTreeBuilder.class);

    private static final String ON_ERROR_BRANCH = "on-error-branch";
    private static final int ON_ERROR_BRANCH_COUNT = 2;

    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final Supplier<BiFunction<TaskExecution, JobDTO, TaskExecutionDTO>> taskExecutionDTOConverterFactory;

    LazyTaskExecutionTreeBuilder(
        TaskExecutionService taskExecutionService, JobService jobService,
        Supplier<BiFunction<TaskExecution, JobDTO, TaskExecutionDTO>> taskExecutionDTOConverterFactory) {

        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskExecutionDTOConverterFactory = taskExecutionDTOConverterFactory;
    }

    long countTaskExecutions(long jobId, List<TaskExecution> topLevelTaskExecutions) {
        Set<Long> latestAttemptIds = getLatestAttemptIds(topLevelTaskExecutions);

        List<Long> supersededTaskExecutionIds = topLevelTaskExecutions.stream()
            .map(TaskExecution::getId)
            .filter(id -> !latestAttemptIds.contains(id))
            .toList();

        return taskExecutionService.countJobTaskExecutions(jobId, supersededTaskExecutionIds);
    }

    List<TaskExecutionDTO> getTopLevelTaskExecutions(long jobId) {
        return decorate(getTopLevelTaskExecutionRows(jobId));
    }

    List<TaskExecution> getTopLevelTaskExecutionRows(long jobId) {
        return taskExecutionService.getJobTopLevelTaskExecutions(jobId);
    }

    List<TaskExecutionDTO> decorate(List<TaskExecution> taskExecutions) {
        if (taskExecutions.isEmpty()) {
            return List.of();
        }

        List<Long> ids = taskExecutions.stream()
            .map(TaskExecution::getId)
            .toList();

        Map<Long, Long> childCounts = taskExecutionService.getChildCounts(ids);
        Map<Long, Long> iterationCounts = taskExecutionService.getIterationCounts(ids);
        Map<Long, Job> childJobMap = getChildJobMap(ids);

        BiFunction<TaskExecution, JobDTO, TaskExecutionDTO> toTaskExecutionDTO = taskExecutionDTOConverterFactory.get();

        List<TaskExecutionDTO> taskExecutionDTOs = new ArrayList<>();

        for (TaskExecution taskExecution : taskExecutions) {
            Job childJob = childJobMap.get(taskExecution.getId());

            TaskExecutionDTO taskExecutionDTO = toTaskExecutionDTO.apply(
                taskExecution, childJob == null ? null : JobDTO.shallow(childJob));

            taskExecutionDTOs.add(
                taskExecutionDTO.withChildCount(getChildCount(taskExecution, childCounts, iterationCounts)));
        }

        return taskExecutionDTOs;
    }

    @Nullable
    JobDTO getChildJob(long taskExecutionId) {
        Map<Long, Job> childJobMap = getChildJobMap(List.of(taskExecutionId));

        Job childJob = childJobMap.get(taskExecutionId);

        return childJob == null ? null : JobDTO.shallow(childJob);
    }

    TaskExecutionChildrenPageDTO getChildrenPage(TaskExecution parent, int pageNumber, int pageSize) {
        Validate.isTrue(pageNumber >= 0, "pageNumber must not be negative");
        Validate.isTrue(pageSize > 0, "pageSize must be positive");
        Validate.isTrue(pageSize <= MAX_PAGE_SIZE, "pageSize must not exceed " + MAX_PAGE_SIZE);

        String type = parent.getType();

        if (TaskExecutionDTO.isOnErrorType(type)) {
            return getOnErrorChildrenPage(parent, pageNumber, pageSize);
        }

        if (TaskExecutionDTO.isLoopType(type)) {
            return getLoopChildrenPage(parent, pageNumber, pageSize);
        }

        return getPlainChildrenPage(parent, pageNumber, pageSize);
    }

    List<FocusPathElementDTO> getFocusPath(long jobId, List<TaskExecution> topLevelTaskExecutions) {
        Set<Long> latestAttemptIds = getLatestAttemptIds(topLevelTaskExecutions);
        Map<Long, Optional<TaskExecution>> ancestorCache = new HashMap<>();

        Optional<List<TaskExecution>> failedAncestryOptional = fetchFailedLeafAncestry(
            jobId, latestAttemptIds, ancestorCache);

        if (failedAncestryOptional.isPresent()) {
            return toFocusPath(failedAncestryOptional.get());
        }

        Optional<TaskExecution> lastTaskExecutionOptional = taskExecutionService
            .fetchLastStartedJobTaskExecution(jobId)
            .or(() -> taskExecutionService.fetchLastJobTaskExecution(jobId));

        return lastTaskExecutionOptional
            .flatMap(lastTaskExecution -> fetchAncestry(lastTaskExecution, ancestorCache))
            .map(this::toFocusPath)
            .orElse(List.of());
    }

    private Optional<List<TaskExecution>> fetchFailedLeafAncestry(
        long jobId, Set<Long> latestAttemptIds, Map<Long, Optional<TaskExecution>> ancestorCache) {

        int pageNumber = 0;
        List<TaskExecution> failedTaskExecutions;

        do {
            failedTaskExecutions = taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(
                jobId, pageNumber, FOCUS_CANDIDATE_PAGE_SIZE);

            for (TaskExecution failedTaskExecution : failedTaskExecutions) {
                Optional<List<TaskExecution>> ancestryOptional = fetchAncestry(failedTaskExecution, ancestorCache);

                if (ancestryOptional.isEmpty()) {
                    continue;
                }

                List<TaskExecution> ancestry = ancestryOptional.get();

                TaskExecution topLevelTaskExecution = ancestry.getFirst();

                if (latestAttemptIds.contains(topLevelTaskExecution.getId())) {
                    return Optional.of(ancestry);
                }
            }

            pageNumber++;
        } while (failedTaskExecutions.size() == FOCUS_CANDIDATE_PAGE_SIZE);

        return Optional.empty();
    }

    private Optional<List<TaskExecution>> fetchAncestry(
        TaskExecution taskExecution, Map<Long, Optional<TaskExecution>> ancestorCache) {

        List<TaskExecution> ancestry = new ArrayList<>();

        TaskExecution currentTaskExecution = taskExecution;

        ancestry.add(currentTaskExecution);

        while (currentTaskExecution.getParentId() != null) {
            Long parentId = currentTaskExecution.getParentId();

            Optional<TaskExecution> parentOptional = ancestorCache.computeIfAbsent(
                parentId, taskExecutionService::fetchTaskExecution);

            if (parentOptional.isEmpty()) {
                log.warn(
                    "Parent task execution id={} of task execution id={} not found", parentId,
                    currentTaskExecution.getId());

                return Optional.empty();
            }

            currentTaskExecution = parentOptional.get();

            ancestry.add(currentTaskExecution);
        }

        Collections.reverse(ancestry);

        return Optional.of(ancestry);
    }

    private int getChildCount(
        TaskExecution taskExecution, Map<Long, Long> childCounts, Map<Long, Long> iterationCounts) {

        String type = taskExecution.getType();
        Long id = taskExecution.getId();

        if (TaskExecutionDTO.isOnErrorType(type)) {
            return childCounts.getOrDefault(id, 0L) > 0 ? ON_ERROR_BRANCH_COUNT : 0;
        }

        if (TaskExecutionDTO.isLoopType(type)) {
            return Math.toIntExact(iterationCounts.getOrDefault(id, 0L));
        }

        return Math.toIntExact(childCounts.getOrDefault(id, 0L));
    }

    private Map<Long, Job> getChildJobMap(List<Long> taskExecutionIds) {
        return jobService.getChildJobs(taskExecutionIds)
            .stream()
            .filter(childJob -> childJob.getParentTaskExecutionId() != null)
            .collect(
                Collectors.toMap(
                    Job::getParentTaskExecutionId, Function.identity(), LazyTaskExecutionTreeBuilder::getLatestJob));
    }

    private static Job getLatestJob(Job job, Job otherJob) {
        log.warn(
            "Task execution id={} has more than one child job, ids={} and {}", job.getParentTaskExecutionId(),
            job.getId(), otherJob.getId());

        return Comparator.nullsFirst(Comparator.<Long>naturalOrder())
            .compare(job.getId(), otherJob.getId()) >= 0 ? job : otherJob;
    }

    private @Nullable Integer getChildIndex(TaskExecution taskExecution, @Nullable TaskExecution parent) {
        if (parent == null) {
            return null;
        }

        String parentType = parent.getType();

        if (TaskExecutionDTO.isLoopType(parentType) || TaskExecutionDTO.isOnErrorType(parentType)) {
            return null;
        }

        return taskExecutionService.getChildIndex(taskExecution);
    }

    private @Nullable Integer getIterationIndex(TaskExecution taskExecution, @Nullable TaskExecution parent) {
        if (parent == null) {
            return null;
        }

        String parentType = parent.getType();

        if (TaskExecutionDTO.isLoopType(parentType)) {
            return taskExecutionService.getIterationIndex(taskExecution);
        }

        if (TaskExecutionDTO.isOnErrorType(parentType)) {
            return isErrorBranchTaskExecution(taskExecution, getErrorBranchTaskNames(parent)) ? 1 : 0;
        }

        return null;
    }

    private static Set<String> getErrorBranchTaskNames(TaskExecution onErrorTaskExecution) {
        return TaskExecutionDTO.getBranchTaskNames(onErrorTaskExecution.getWorkflowTask(), ON_ERROR_BRANCH);
    }

    private static Set<Long> getLatestAttemptIds(List<TaskExecution> topLevelTaskExecutions) {
        Map<String, Long> latestAttemptIdMap = new HashMap<>();
        Set<Long> latestAttemptIds = new HashSet<>();

        for (TaskExecution topLevelTaskExecution : topLevelTaskExecutions) {
            String workflowTaskName = getWorkflowTaskName(topLevelTaskExecution);

            if (workflowTaskName == null) {
                latestAttemptIds.add(topLevelTaskExecution.getId());
            } else {
                latestAttemptIdMap.put(workflowTaskName, topLevelTaskExecution.getId());
            }
        }

        latestAttemptIds.addAll(latestAttemptIdMap.values());

        return latestAttemptIds;
    }

    private TaskExecutionChildrenPageDTO getLoopChildrenPage(TaskExecution parent, int pageNumber, int pageSize) {
        long parentId = Objects.requireNonNull(parent.getId());

        List<TaskExecution> firstChildTaskExecutions = taskExecutionService.getParentTaskExecutions(parentId, 0, 1);

        if (firstChildTaskExecutions.isEmpty()) {
            return new TaskExecutionChildrenPageDTO(List.of(), List.of(), pageNumber, pageSize, 0, 0, 0);
        }

        TaskExecution firstChildTaskExecution = firstChildTaskExecutions.getFirst();

        int leadingIterationCount = isIterationStart(firstChildTaskExecution) ? 0 : 1;

        Map<Long, Long> iterationCounts = taskExecutionService.getIterationCounts(List.of(parentId));

        long totalElements = iterationCounts.getOrDefault(parentId, 0L);
        long fromIndex = (long) pageNumber * pageSize;

        List<List<TaskExecutionDTO>> iterations = fromIndex < totalElements
            ? getLoopPageIterations(parentId, firstChildTaskExecution, leadingIterationCount, fromIndex, pageSize)
            : List.of();

        return new TaskExecutionChildrenPageDTO(
            List.of(), iterations, pageNumber, pageSize, iterations.size(), totalElements,
            getTotalPages(totalElements, pageSize));
    }

    private List<List<TaskExecutionDTO>> getLoopPageIterations(
        long parentId, TaskExecution firstChildTaskExecution, int leadingIterationCount, long fromIndex,
        int pageSize) {

        boolean startsAtFirstChild = fromIndex < leadingIterationCount;

        long iterationStartOffset = startsAtFirstChild ? 0 : fromIndex - leadingIterationCount;
        int pageIterationStartCount = startsAtFirstChild ? pageSize - leadingIterationCount : pageSize;

        List<TaskExecution> iterationStartTaskExecutions = taskExecutionService.getParentIterationStartTaskExecutions(
            parentId, iterationStartOffset, pageIterationStartCount + 1);

        if (!startsAtFirstChild && iterationStartTaskExecutions.isEmpty()) {
            return List.of();
        }

        TaskExecution fromTaskExecution = startsAtFirstChild
            ? firstChildTaskExecution : iterationStartTaskExecutions.getFirst();

        List<TaskExecution> pageTaskExecutions;

        if (iterationStartTaskExecutions.size() > pageIterationStartCount) {
            pageTaskExecutions = taskExecutionService.getParentTaskExecutionsBetween(
                parentId, fromTaskExecution, iterationStartTaskExecutions.get(pageIterationStartCount));
        } else {
            pageTaskExecutions = taskExecutionService.getParentTaskExecutionsFrom(parentId, fromTaskExecution);
        }

        List<List<TaskExecutionDTO>> iterations = splitIterations(pageTaskExecutions, decorate(pageTaskExecutions));

        return iterations.size() > pageSize ? iterations.subList(0, pageSize) : iterations;
    }

    private TaskExecutionChildrenPageDTO getOnErrorChildrenPage(TaskExecution parent, int pageNumber, int pageSize) {
        long parentId = Objects.requireNonNull(parent.getId());

        List<Long> childIds = taskExecutionService.getParentTaskExecutionIds(parentId);

        if (childIds.isEmpty()) {
            return new TaskExecutionChildrenPageDTO(List.of(), List.of(), pageNumber, pageSize, 0, 0, 0);
        }

        if (pageNumber > 0) {
            return new TaskExecutionChildrenPageDTO(
                List.of(), List.of(), pageNumber, pageSize, 0, ON_ERROR_BRANCH_COUNT, 1);
        }

        Set<String> errorBranchTaskNames = getErrorBranchTaskNames(parent);

        List<TaskExecution> childTaskExecutions = taskExecutionService.getTaskExecutions(childIds);

        List<TaskExecutionDTO> childTaskExecutionDTOs = decorate(childTaskExecutions);

        List<TaskExecutionDTO> mainBranchTaskExecutionDTOs = new ArrayList<>();
        List<TaskExecutionDTO> errorBranchTaskExecutionDTOs = new ArrayList<>();

        for (int index = 0; index < childTaskExecutions.size(); index++) {
            if (isErrorBranchTaskExecution(childTaskExecutions.get(index), errorBranchTaskNames)) {
                errorBranchTaskExecutionDTOs.add(childTaskExecutionDTOs.get(index));
            } else {
                mainBranchTaskExecutionDTOs.add(childTaskExecutionDTOs.get(index));
            }
        }

        return new TaskExecutionChildrenPageDTO(
            List.of(), List.of(mainBranchTaskExecutionDTOs, errorBranchTaskExecutionDTOs), pageNumber, pageSize,
            ON_ERROR_BRANCH_COUNT, ON_ERROR_BRANCH_COUNT, 1);
    }

    private TaskExecutionChildrenPageDTO getPlainChildrenPage(TaskExecution parent, int pageNumber, int pageSize) {
        long parentId = Objects.requireNonNull(parent.getId());

        List<TaskExecutionDTO> children = decorate(
            taskExecutionService.getParentTaskExecutions(parentId, pageNumber, pageSize));

        Map<Long, Long> childCounts = taskExecutionService.getChildCounts(List.of(parentId));

        long totalElements = childCounts.getOrDefault(parentId, 0L);

        return new TaskExecutionChildrenPageDTO(
            children, List.of(), pageNumber, pageSize, children.size(), totalElements,
            getTotalPages(totalElements, pageSize));
    }

    private static int getTotalPages(long totalElements, int pageSize) {
        return Math.toIntExact((totalElements + pageSize - 1) / pageSize);
    }

    private static @Nullable String getWorkflowTaskName(TaskExecution taskExecution) {
        WorkflowTask workflowTask = taskExecution.getWorkflowTask();

        return workflowTask == null ? null : workflowTask.getName();
    }

    private static boolean isErrorBranchTaskExecution(TaskExecution taskExecution, Set<String> errorBranchTaskNames) {
        String workflowTaskName = getWorkflowTaskName(taskExecution);

        return workflowTaskName != null && errorBranchTaskNames.contains(workflowTaskName);
    }

    private static boolean isIterationStart(TaskExecution taskExecution) {
        return taskExecution.getTaskNumber() == 0;
    }

    private static List<List<TaskExecutionDTO>> splitIterations(
        List<TaskExecution> taskExecutions, List<TaskExecutionDTO> taskExecutionDTOs) {

        List<List<TaskExecutionDTO>> iterations = new ArrayList<>();
        List<TaskExecutionDTO> currentIteration = new ArrayList<>();

        for (int index = 0; index < taskExecutions.size(); index++) {
            if (isIterationStart(taskExecutions.get(index)) && !currentIteration.isEmpty()) {
                iterations.add(currentIteration);

                currentIteration = new ArrayList<>();
            }

            currentIteration.add(taskExecutionDTOs.get(index));
        }

        if (!currentIteration.isEmpty()) {
            iterations.add(currentIteration);
        }

        return iterations;
    }

    private List<FocusPathElementDTO> toFocusPath(List<TaskExecution> ancestry) {
        List<FocusPathElementDTO> focusPath = new ArrayList<>();

        TaskExecution parent = null;

        for (TaskExecution taskExecution : ancestry) {
            focusPath.add(
                new FocusPathElementDTO(
                    Objects.requireNonNull(taskExecution.getId()), getIterationIndex(taskExecution, parent),
                    getChildIndex(taskExecution, parent)));

            parent = taskExecution;
        }

        return focusPath;
    }
}
