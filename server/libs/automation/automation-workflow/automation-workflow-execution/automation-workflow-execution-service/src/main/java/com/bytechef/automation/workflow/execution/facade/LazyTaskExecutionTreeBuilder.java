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

/**
 * @author Ivica Cardic
 */
class LazyTaskExecutionTreeBuilder {

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

    List<TaskExecutionDTO> getTopLevelTaskExecutions(long jobId) {
        return decorate(getTopLevelTaskExecutionRows(jobId), jobId);
    }

    List<TaskExecution> getTopLevelTaskExecutionRows(long jobId) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getParentId() == null)
            .toList();
    }

    List<TaskExecutionDTO> decorate(List<TaskExecution> taskExecutions, long jobId) {
        if (taskExecutions.isEmpty()) {
            return List.of();
        }

        List<Long> ids = taskExecutions.stream()
            .map(TaskExecution::getId)
            .toList();

        Map<Long, Long> childCounts = taskExecutionService.getChildCounts(ids);
        Map<Long, Long> iterationCounts = taskExecutionService.getIterationCounts(ids);
        Map<Long, Job> childJobMap = getChildJobMap(jobId);

        BiFunction<TaskExecution, JobDTO, TaskExecutionDTO> toTaskExecutionDTO = taskExecutionDTOConverterFactory.get();

        List<TaskExecutionDTO> taskExecutionDTOs = new ArrayList<>();

        for (TaskExecution taskExecution : taskExecutions) {
            Job childJob = childJobMap.get(taskExecution.getId());

            TaskExecutionDTO taskExecutionDTO = toTaskExecutionDTO.apply(
                taskExecution, childJob == null ? null : new JobDTO(childJob));

            taskExecutionDTOs.add(
                taskExecutionDTO.withChildCount(getChildCount(taskExecution, childCounts, iterationCounts)));
        }

        return taskExecutionDTOs;
    }

    TaskExecutionChildrenPageDTO getChildrenPage(TaskExecution parent, int pageNumber, int pageSize) {
        Validate.isTrue(pageNumber >= 0, "pageNumber must not be negative");
        Validate.isTrue(pageSize > 0, "pageSize must be positive");

        String type = parent.getType();

        if (TaskExecutionDTO.isOnErrorType(type)) {
            return getOnErrorChildrenPage(parent, pageSize);
        }

        if (TaskExecutionDTO.isLoopType(type)) {
            return getLoopChildrenPage(parent, pageNumber, pageSize);
        }

        return getPlainChildrenPage(parent, pageNumber, pageSize);
    }

    List<FocusPathElementDTO> getFocusPath(long jobId, List<TaskExecution> topLevelTaskExecutions) {
        Set<Long> latestAttemptIds = getLatestAttemptIds(topLevelTaskExecutions);
        Map<Long, TaskExecution> ancestorCache = new HashMap<>();

        List<TaskExecution> failedTaskExecutions = taskExecutionService.getFailedJobTaskExecutions(jobId);

        if (!failedTaskExecutions.isEmpty()) {
            Map<Long, Long> childCounts = taskExecutionService.getChildCounts(
                failedTaskExecutions.stream()
                    .map(TaskExecution::getId)
                    .toList());

            for (TaskExecution failedTaskExecution : failedTaskExecutions) {
                if (childCounts.getOrDefault(failedTaskExecution.getId(), 0L) > 0) {
                    continue;
                }

                List<TaskExecution> ancestry = getAncestry(failedTaskExecution, ancestorCache);

                TaskExecution topLevelTaskExecution = ancestry.getFirst();

                if (latestAttemptIds.contains(topLevelTaskExecution.getId())) {
                    return toFocusPath(ancestry);
                }
            }
        }

        Optional<TaskExecution> lastTaskExecutionOptional = taskExecutionService.fetchLastJobTaskExecution(jobId);

        return lastTaskExecutionOptional
            .map(lastTaskExecution -> toFocusPath(getAncestry(lastTaskExecution, ancestorCache)))
            .orElse(List.of());
    }

    private List<TaskExecution> getAncestry(TaskExecution taskExecution, Map<Long, TaskExecution> ancestorCache) {
        List<TaskExecution> ancestry = new ArrayList<>();

        TaskExecution currentTaskExecution = taskExecution;

        ancestry.add(currentTaskExecution);

        while (currentTaskExecution.getParentId() != null) {
            currentTaskExecution = ancestorCache.computeIfAbsent(
                currentTaskExecution.getParentId(), taskExecutionService::getTaskExecution);

            ancestry.add(currentTaskExecution);
        }

        Collections.reverse(ancestry);

        return ancestry;
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

    private Map<Long, Job> getChildJobMap(long jobId) {
        List<Long> childJobIds = jobService.getChildJobIds(jobId);

        if (childJobIds.isEmpty()) {
            return Map.of();
        }

        return jobService.getJobs(childJobIds)
            .stream()
            .filter(childJob -> childJob.getParentTaskExecutionId() != null)
            .collect(
                Collectors.toMap(
                    Job::getParentTaskExecutionId, Function.identity(), (firstJob, otherJob) -> firstJob));
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

        List<List<Long>> iterationIds = splitIterations(
            taskExecutionService.getParentTaskExecutionIds(parentId),
            new HashSet<>(taskExecutionService.getParentIterationStartIds(parentId)));

        int fromIndex = (int) Math.min((long) pageNumber * pageSize, iterationIds.size());
        int toIndex = (int) Math.min((long) fromIndex + pageSize, iterationIds.size());

        List<List<Long>> pageIterationIds = iterationIds.subList(fromIndex, toIndex);

        List<TaskExecutionDTO> taskExecutionDTOs = pageIterationIds.isEmpty()
            ? List.of()
            : decorate(
                taskExecutionService.getTaskExecutions(
                    pageIterationIds.stream()
                        .flatMap(List::stream)
                        .toList()),
                Objects.requireNonNull(parent.getJobId()));

        Map<Long, TaskExecutionDTO> taskExecutionDTOMap = taskExecutionDTOs.stream()
            .collect(Collectors.toMap(TaskExecutionDTO::id, Function.identity()));

        List<List<TaskExecutionDTO>> iterations = pageIterationIds.stream()
            .map(iteration -> iteration.stream()
                .map(taskExecutionDTOMap::get)
                .filter(Objects::nonNull)
                .toList())
            .toList();

        return new TaskExecutionChildrenPageDTO(
            List.of(), iterations, pageNumber, pageSize, iterations.size(), iterationIds.size(),
            getTotalPages(iterationIds.size(), pageSize));
    }

    private TaskExecutionChildrenPageDTO getOnErrorChildrenPage(TaskExecution parent, int pageSize) {
        long parentId = Objects.requireNonNull(parent.getId());

        List<Long> childIds = taskExecutionService.getParentTaskExecutionIds(parentId);

        if (childIds.isEmpty()) {
            return new TaskExecutionChildrenPageDTO(List.of(), List.of(), 0, pageSize, 0, 0, 0);
        }

        Set<String> errorBranchTaskNames = getErrorBranchTaskNames(parent);

        List<TaskExecution> childTaskExecutions = taskExecutionService.getTaskExecutions(childIds);

        List<TaskExecutionDTO> childTaskExecutionDTOs = decorate(
            childTaskExecutions, Objects.requireNonNull(parent.getJobId()));

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
            List.of(), List.of(mainBranchTaskExecutionDTOs, errorBranchTaskExecutionDTOs), 0, pageSize,
            ON_ERROR_BRANCH_COUNT, ON_ERROR_BRANCH_COUNT, 1);
    }

    private TaskExecutionChildrenPageDTO getPlainChildrenPage(TaskExecution parent, int pageNumber, int pageSize) {
        long parentId = Objects.requireNonNull(parent.getId());

        List<TaskExecutionDTO> children = decorate(
            taskExecutionService.getParentTaskExecutions(parentId, pageNumber, pageSize),
            Objects.requireNonNull(parent.getJobId()));

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

    private static List<List<Long>> splitIterations(List<Long> childIds, Set<Long> iterationStartIds) {
        List<List<Long>> iterations = new ArrayList<>();
        List<Long> currentIteration = new ArrayList<>();

        for (Long childId : childIds) {
            if (iterationStartIds.contains(childId) && !currentIteration.isEmpty()) {
                iterations.add(currentIteration);

                currentIteration = new ArrayList<>();
            }

            currentIteration.add(childId);
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
                    Objects.requireNonNull(taskExecution.getId()), getIterationIndex(taskExecution, parent)));

            parent = taskExecution;
        }

        return focusPath;
    }
}
