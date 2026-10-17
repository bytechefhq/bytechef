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

package com.bytechef.automation.configuration.validator;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.workflow.validator.WorkflowDeploymentConnectionResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ProjectDeploymentWorkflowConnectionResolver implements WorkflowDeploymentConnectionResolver {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    ProjectDeploymentWorkflowConnectionResolver(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public List<WorkflowNodeConnection> getWorkflowNodeConnections(
        String workflowId, String workflowNodeName, long environmentId) {

        List<WorkflowNodeConnection> workflowNodeConnections = new ArrayList<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getWorkflowProjectDeploymentWorkflows(workflowId)) {

            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                projectDeploymentWorkflow.getProjectDeploymentId());

            if (projectDeployment.getEnvironmentId() != environmentId) {
                continue;
            }

            for (ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection : projectDeploymentWorkflow
                .getConnections()) {

                if (Objects.equals(projectDeploymentWorkflowConnection.getWorkflowNodeName(), workflowNodeName)) {
                    workflowNodeConnections.add(
                        new WorkflowNodeConnection(
                            projectDeploymentWorkflowConnection.getConnectionId(),
                            projectDeploymentWorkflowConnection.getWorkflowConnectionKey()));
                }
            }
        }

        return workflowNodeConnections;
    }
}
