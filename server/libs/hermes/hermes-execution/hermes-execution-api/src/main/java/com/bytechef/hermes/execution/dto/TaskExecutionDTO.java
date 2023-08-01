
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.execution.dto;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskExecutionDTO(
    ComponentDefinitionDTO componentDefinition, String createdBy, LocalDateTime createdDate, LocalDateTime endDate,
    ExecutionError error, long executionTime, Long id, Map<String, ?> input, Long jobId, String lastModifiedBy,
    LocalDateTime lastModifiedDate, int maxRetries, Object output, Long parentId, int priority, int progress,
    int retryAttempts, String retryDelay, int retryDelayFactor, long retryDelayMillis, LocalDateTime startDate,
    TaskExecution.Status status, int taskNumber, String type, WorkflowTask workflowTask) {

    public TaskExecutionDTO(
        ComponentDefinitionDTO componentDefinition, Map<String, ?> input, TaskExecution taskExecution) {

        this(
            componentDefinition, taskExecution.getCreatedBy(), taskExecution.getCreatedDate(),
            taskExecution.getEndDate(), taskExecution.getError(), taskExecution.getExecutionTime(),
            taskExecution.getId(), input, taskExecution.getJobId(), taskExecution.getLastModifiedBy(),
            taskExecution.getLastModifiedDate(), taskExecution.getMaxRetries(), taskExecution.getOutput(),
            taskExecution.getParentId(), taskExecution.getPriority(), taskExecution.getProgress(),
            taskExecution.getRetryAttempts(), taskExecution.getRetryDelay(), taskExecution.getRetryDelayFactor(),
            taskExecution.getRetryDelayMillis(), taskExecution.getStartDate(), taskExecution.getStatus(),
            taskExecution.getTaskNumber(), taskExecution.getType(), taskExecution.getWorkflowTask());
    }
}
