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

package com.bytechef.platform.workflow.execution.dto;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.fasterxml.jackson.annotation.JsonFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskExecutionDTO(
    String createdBy, Instant createdDate, Instant endDate, ExecutionError error, long executionTime,
    String icon, @JsonFormat(shape = JsonFormat.Shape.STRING) Long id, Map<String, ?> input,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Long jobId, String lastModifiedBy, Instant lastModifiedDate,
    int maxRetries, Object output, @JsonFormat(shape = JsonFormat.Shape.STRING) Long parentId, int priority,
    int progress, int retryAttempts, String retryDelay, int retryDelayFactor, long retryDelayMillis,
    Instant startDate, TaskExecution.Status status, int taskNumber, String title, String type,
    WorkflowTask workflowTask, List<TaskExecutionDTO> children, List<List<TaskExecutionDTO>> iterations) {

    public TaskExecutionDTO(
        TaskExecution taskExecution, String title, String icon, Map<String, ?> input, Object output) {

        this(
            taskExecution.getCreatedBy(), taskExecution.getCreatedDate(),
            taskExecution.getEndDate(), taskExecution.getError(), taskExecution.getExecutionTime(), icon,
            taskExecution.getId(), input, taskExecution.getJobId(), taskExecution.getLastModifiedBy(),
            taskExecution.getLastModifiedDate(), taskExecution.getMaxRetries(), output,
            taskExecution.getParentId(), taskExecution.getPriority(), taskExecution.getProgress(),
            taskExecution.getRetryAttempts(), taskExecution.getRetryDelay(), taskExecution.getRetryDelayFactor(),
            taskExecution.getRetryDelayMillis(), taskExecution.getStartDate(), taskExecution.getStatus(),
            taskExecution.getTaskNumber(), title, taskExecution.getType(), taskExecution.getWorkflowTask(),
            List.of(), List.of());
    }

    private TaskExecutionDTO(
        TaskExecutionDTO taskExecutionDTO, List<TaskExecutionDTO> children, List<List<TaskExecutionDTO>> iterations) {

        this(
            taskExecutionDTO.createdBy(), taskExecutionDTO.createdDate(), taskExecutionDTO.endDate(),
            taskExecutionDTO.error(), taskExecutionDTO.executionTime(), taskExecutionDTO.icon(), taskExecutionDTO.id(),
            taskExecutionDTO.input(), taskExecutionDTO.jobId(), taskExecutionDTO.lastModifiedBy(),
            taskExecutionDTO.lastModifiedDate(), taskExecutionDTO.maxRetries(), taskExecutionDTO.output(),
            taskExecutionDTO.parentId(), taskExecutionDTO.priority(), taskExecutionDTO.progress(),
            taskExecutionDTO.retryAttempts(), taskExecutionDTO.retryDelay(), taskExecutionDTO.retryDelayFactor(),
            taskExecutionDTO.retryDelayMillis(), taskExecutionDTO.startDate(), taskExecutionDTO.status(),
            taskExecutionDTO.taskNumber(), taskExecutionDTO.title(), taskExecutionDTO.type(),
            taskExecutionDTO.workflowTask(), children, iterations);
    }

    public static List<TaskExecutionDTO> buildHierarchy(List<TaskExecutionDTO> taskExecutionDTOS) {
        if (taskExecutionDTOS == null || taskExecutionDTOS.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TaskExecutionDTO>> tasksMap = taskExecutionDTOS.stream()
            .filter(taskExecutionDTO -> taskExecutionDTO.parentId() != null)
            .collect(Collectors.groupingBy(TaskExecutionDTO::parentId));

        List<TaskExecutionDTO> topLevelTasks = taskExecutionDTOS.stream()
            .filter(taskExecutionDTO -> taskExecutionDTO.parentId() == null)
            .toList();

        return topLevelTasks.stream()
            .map(taskExecutionDTO -> buildTasksTree(taskExecutionDTO, tasksMap))
            .toList();
    }

    private static TaskExecutionDTO buildTasksTree(
        TaskExecutionDTO taskExecutionDTO, Map<Long, List<TaskExecutionDTO>> tasksMap) {

        List<TaskExecutionDTO> matchingChildren = tasksMap.getOrDefault(taskExecutionDTO.id(), List.of());
        String type = taskExecutionDTO.type();

        if (type != null && type.toLowerCase()
            .contains("loop")) {
            List<List<TaskExecutionDTO>> iterationItems = new ArrayList<>();
            List<TaskExecutionDTO> currentIterationItems = new ArrayList<>();
            Integer previousTaskNumber = null;

            for (TaskExecutionDTO child : matchingChildren) {
                if (child.taskNumber() == 0 && previousTaskNumber != null && !currentIterationItems.isEmpty()) {
                    iterationItems.add(new ArrayList<>(currentIterationItems));

                    currentIterationItems.clear();
                }

                currentIterationItems.add(buildTasksTree(child, tasksMap));

                previousTaskNumber = child.taskNumber();
            }

            if (!currentIterationItems.isEmpty()) {
                iterationItems.add(new ArrayList<>(currentIterationItems));
            }

            return new TaskExecutionDTO(taskExecutionDTO, List.of(), iterationItems);
        }

        List<TaskExecutionDTO> children = matchingChildren.stream()
            .map(child -> buildTasksTree(child, tasksMap))
            .toList();

        return new TaskExecutionDTO(taskExecutionDTO, children, List.of());
    }
}
