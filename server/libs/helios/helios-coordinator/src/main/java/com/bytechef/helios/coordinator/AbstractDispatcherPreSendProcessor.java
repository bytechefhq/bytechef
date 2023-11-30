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

package com.bytechef.helios.coordinator;

import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.configuration.domain.WorkflowConnection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractDispatcherPreSendProcessor {

    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;

    protected AbstractDispatcherPreSendProcessor(ProjectInstanceWorkflowService projectInstanceWorkflowService) {
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
    }

    protected Map<String, Long> getConnectionIdMap(
        Long projectInstanceId, String workflowId, List<WorkflowConnection> workflowConnections) {

        return workflowConnections.stream()
            .collect(Collectors.toMap(
                WorkflowConnection::getKey,
                workflowConnection -> getConnectionId(projectInstanceId, workflowId, workflowConnection)));
    }

    private Long getConnectionId(Long projectInstanceId, String workflowId, WorkflowConnection workflowConnection) {
        return projectInstanceWorkflowService.getProjectInstanceWorkflowConnectionId(
            projectInstanceId, workflowId, workflowConnection.getOperationName(),
            workflowConnection.getKey());
    }
}
