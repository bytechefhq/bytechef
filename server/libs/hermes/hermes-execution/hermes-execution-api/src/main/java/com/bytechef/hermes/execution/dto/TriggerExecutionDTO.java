/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.component.registry.domain.ComponentDefinition;
import com.bytechef.hermes.configuration.domain.WorkflowTrigger;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.execution.domain.TriggerExecution.Status;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TriggerExecutionDTO(
    boolean batch, ComponentDefinition component, String createdBy, LocalDateTime createdDate,
    LocalDateTime endDate, ExecutionError error, long executionTime, Long id, Map<String, ?> input,
    String lastModifiedBy, LocalDateTime lastModifiedDate, int maxRetries, Object output, int priority,
    int retryAttempts, String retryDelay, int retryDelayFactor, long retryDelayMillis, LocalDateTime startDate,
    Object state, Status status, WorkflowExecutionId workflowExecutionId, String type,
    WorkflowTrigger workflowTrigger) {

    public TriggerExecutionDTO(
        ComponentDefinition component, Map<String, ?> input, Object output, TriggerExecution triggerExecution) {

        this(
            triggerExecution.isBatch(), component, triggerExecution.getCreatedBy(),
            triggerExecution.getCreatedDate(), triggerExecution.getEndDate(), triggerExecution.getError(),
            triggerExecution.getExecutionTime(), triggerExecution.getId(), input, triggerExecution.getLastModifiedBy(),
            triggerExecution.getLastModifiedDate(), triggerExecution.getMaxRetries(), output,
            triggerExecution.getPriority(), triggerExecution.getRetryAttempts(), triggerExecution.getRetryDelay(),
            triggerExecution.getRetryDelayFactor(), triggerExecution.getRetryDelayMillis(),
            triggerExecution.getStartDate(), triggerExecution.getState(), triggerExecution.getStatus(),
            triggerExecution.getWorkflowExecutionId(), triggerExecution.getType(),
            triggerExecution.getWorkflowTrigger());
    }
}
