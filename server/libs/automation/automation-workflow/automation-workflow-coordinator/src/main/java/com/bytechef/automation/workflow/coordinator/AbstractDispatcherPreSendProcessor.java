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

package com.bytechef.automation.workflow.coordinator;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.MapUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractDispatcherPreSendProcessor {

    protected final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    protected AbstractDispatcherPreSendProcessor(ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    protected Map<String, Long> getConnectionIdMap(
        Long projectDeploymentId, String workflowId, String workflowNodeName) {

        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflowConnections(
                projectDeploymentId, workflowId, workflowNodeName);

        return MapUtils.toMap(
            projectDeploymentWorkflowConnections, ProjectDeploymentWorkflowConnection::getKey,
            ProjectDeploymentWorkflowConnection::getConnectionId);
    }
}
