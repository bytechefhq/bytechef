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

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectDeploymentWorkflowDTO(
    List<ProjectDeploymentWorkflowConnection> connections, String createdBy, Instant createdDate,
    Map<String, ?> inputs, boolean enabled, Long id, Instant lastExecutionDate, String lastModifiedBy,
    Instant lastModifiedDate, Long projectDeploymentId, String staticWebhookUrl, int version, String workflowId,
    String workflowReferenceCode)
    implements Comparable<ProjectDeploymentWorkflowDTO> {

    public ProjectDeploymentWorkflowDTO(
        ProjectDeploymentWorkflow projectDeploymentWorkflow, Instant lastExecutionDate, String staticWebhookUrl,
        String workflowReferenceCode) {

        this(
            projectDeploymentWorkflow.getConnections(), projectDeploymentWorkflow.getCreatedBy(),
            projectDeploymentWorkflow.getCreatedDate(), projectDeploymentWorkflow.getInputs(),
            projectDeploymentWorkflow.isEnabled(), projectDeploymentWorkflow.getId(),
            lastExecutionDate, projectDeploymentWorkflow.getLastModifiedBy(),
            projectDeploymentWorkflow.getLastModifiedDate(), projectDeploymentWorkflow.getProjectDeploymentId(),
            staticWebhookUrl, projectDeploymentWorkflow.getVersion(), projectDeploymentWorkflow.getWorkflowId(),
            workflowReferenceCode);
    }

    @Override
    public int compareTo(ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO) {
        return workflowId.compareTo(projectDeploymentWorkflowDTO.workflowId);
    }

    public ProjectDeploymentWorkflow toProjectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(connections);
        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setId(id);
        projectDeploymentWorkflow.setInputs(inputs);
        projectDeploymentWorkflow.setVersion(version);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }
}
