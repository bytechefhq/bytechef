
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

package com.bytechef.helios.coordinator;

import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.configuration.connection.WorkflowConnection;

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

    protected Map<String, Long> getConnectionIdMap(List<WorkflowConnection> workflowConnections) {
        return workflowConnections.stream()
            .collect(Collectors.toMap(WorkflowConnection::getKey, this::getConnectionId));
    }

    private Long getConnectionId(WorkflowConnection workflowConnection) {
        return workflowConnection.getConnectionId()
            .orElseGet(() -> projectInstanceWorkflowService.getProjectInstanceWorkflowConnectionId(
                workflowConnection.getOperationName(), workflowConnection.getKey()));
    }
}
