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

package com.bytechef.platform.workflow.execution.dto;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.atlas.execution.domain.Job.Webhook;
import com.bytechef.error.ExecutionError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record JobDTO(
    String createdBy, LocalDateTime createdDate, int currentTask, LocalDateTime endDate, ExecutionError error,
    Long id, Map<String, ?> inputs, String label, String lastModifiedBy, LocalDateTime lastModifiedDate,
    Map<String, ?> metadata, Map<String, ?> outputs, Long parentTaskExecutionId, int priority, LocalDateTime startDate,
    Status status, List<TaskExecutionDTO> taskExecutions, int version, List<Webhook> webhooks, String workflowId) {

    public JobDTO(Job job) {
        this(job, Map.of(), List.of());
    }

    public JobDTO(Job job, Map<String, ?> outputs, List<TaskExecutionDTO> taskExecutions) {
        this(job.getCreatedBy(), job.getCreatedDate(), job.getCurrentTask(), job.getEndDate(), job.getError(),
            job.getId(), job.getInputs(), job.getLabel(), job.getLastModifiedBy(), job.getLastModifiedDate(),
            job.getMetadata(), outputs, job.getParentTaskExecutionId(), job.getPriority(), job.getStartDate(),
            job.getStatus(), taskExecutions, job.getVersion(), job.getWebhooks(), job.getWorkflowId());
    }
}
