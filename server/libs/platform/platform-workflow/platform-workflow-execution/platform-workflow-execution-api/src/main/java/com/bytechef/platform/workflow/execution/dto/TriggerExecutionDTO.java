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

import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TriggerExecutionDTO(
    boolean batch, String createdBy, Instant createdDate, Instant endDate, ExecutionError error, long executionTime,
    String icon, Long id, Map<String, ?> input, String lastModifiedBy, Instant lastModifiedDate, int maxRetries,
    Object output, int priority, int retryAttempts, String retryDelay, int retryDelayFactor, long retryDelayMillis,
    Instant startDate, Object state, TriggerExecution.Status status, WorkflowExecutionId workflowExecutionId,
    String title, String type, WorkflowTrigger workflowTrigger) {

    public TriggerExecutionDTO(
        TriggerExecution triggerExecution, String title, String icon, Map<String, ?> input, Object output) {

        this(
            triggerExecution.isBatch(), triggerExecution.getCreatedBy(), triggerExecution.getCreatedDate(),
            triggerExecution.getEndDate(), triggerExecution.getError(), triggerExecution.getExecutionTime(),
            icon, triggerExecution.getId(), input, triggerExecution.getLastModifiedBy(),
            triggerExecution.getLastModifiedDate(), triggerExecution.getMaxRetries(), output,
            triggerExecution.getPriority(), triggerExecution.getRetryAttempts(), triggerExecution.getRetryDelay(),
            triggerExecution.getRetryDelayFactor(), triggerExecution.getRetryDelayMillis(),
            triggerExecution.getStartDate(), triggerExecution.getState(), triggerExecution.getStatus(),
            triggerExecution.getWorkflowExecutionId(), title, triggerExecution.getType(),
            triggerExecution.getWorkflowTrigger());
    }
}
