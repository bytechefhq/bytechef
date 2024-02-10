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

package com.bytechef.automation.configuration.dto;

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflowConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectInstanceWorkflowDTO(
    List<ProjectInstanceWorkflowConnection> connections, String createdBy, LocalDateTime createdDate,
    Map<String, ?> inputs, boolean enabled, Long id, LocalDateTime lastExecutionDate, String lastModifiedBy,
    LocalDateTime lastModifiedDate, Long projectInstanceId, String staticWebhookUrl, int version, String workflowId)
    implements Comparable<ProjectInstanceWorkflowDTO> {

    public ProjectInstanceWorkflowDTO(
        ProjectInstanceWorkflow projectInstanceWorkflow, LocalDateTime lastExecutionDate, String staticWebhookUrl) {

        this(
            projectInstanceWorkflow.getConnections(), projectInstanceWorkflow.getCreatedBy(),
            projectInstanceWorkflow.getCreatedDate(), projectInstanceWorkflow.getInputs(),
            projectInstanceWorkflow.isEnabled(), projectInstanceWorkflow.getProjectInstanceId(),
            lastExecutionDate, projectInstanceWorkflow.getLastModifiedBy(),
            projectInstanceWorkflow.getLastModifiedDate(), projectInstanceWorkflow.getProjectInstanceId(),
            staticWebhookUrl, projectInstanceWorkflow.getVersion(), projectInstanceWorkflow.getWorkflowId());
    }

    @Override
    public int compareTo(ProjectInstanceWorkflowDTO projectInstanceWorkflowDTO) {
        return workflowId.compareTo(projectInstanceWorkflowDTO.workflowId);
    }

    public ProjectInstanceWorkflow toProjectInstanceWorkflow() {
        ProjectInstanceWorkflow projectInstanceWorkflow = new ProjectInstanceWorkflow();

        projectInstanceWorkflow.setConnections(connections);
        projectInstanceWorkflow.setEnabled(enabled);
        projectInstanceWorkflow.setId(id);
        projectInstanceWorkflow.setInputs(inputs);
        projectInstanceWorkflow.setVersion(version);
        projectInstanceWorkflow.setWorkflowId(workflowId);

        return projectInstanceWorkflow;
    }
}
