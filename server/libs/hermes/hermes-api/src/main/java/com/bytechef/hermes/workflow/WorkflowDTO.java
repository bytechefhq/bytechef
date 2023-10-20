
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

package com.bytechef.hermes.workflow;

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.domain.Workflow.Format;
import com.bytechef.atlas.domain.Workflow.Input;
import com.bytechef.atlas.domain.Workflow.Output;
import com.bytechef.atlas.domain.Workflow.SourceType;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.hermes.connection.WorkflowConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowDTO(
    List<WorkflowConnection> connections, String createdBy, LocalDateTime createdDate, String definition,
    String description, ExecutionError error, Format format, String id, List<Input> inputs, String label,
    String lastModifiedBy, LocalDateTime lastModifiedDate, List<Output> outputs, SourceType sourceType, int maxRetries,
    List<WorkflowTask> tasks, int version) {

    public WorkflowDTO(List<WorkflowConnection> connections, Workflow workflow) {
        this(
            connections, workflow.getCreatedBy(), workflow.getCreatedDate(), workflow.getDefinition(),
            workflow.getDescription(), workflow.getError(), workflow.getFormat(), workflow.getId(),
            workflow.getInputs(), workflow.getLabel(), workflow.getLastModifiedBy(), workflow.getLastModifiedDate(),
            workflow.getOutputs(), workflow.getSourceType(), workflow.getMaxRetries(), workflow.getTasks(),
            workflow.getVersion());
    }
}
