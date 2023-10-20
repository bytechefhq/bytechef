
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

package com.bytechef.helios.project.dispatcher;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.connection.WorkflowConnection;

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

    protected Map<String, Long> getConnectionIdMap(Map<String, WorkflowConnection> workflowConnectionMap) {
        return workflowConnectionMap.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> getConnectionId(entry.getValue())));
    }

    private Long getConnectionId(WorkflowConnection workflowConnection) {
        return workflowConnection.getConnectionId()
            .orElseGet(() -> {
                ProjectInstanceWorkflowConnection projectInstanceWorkflowConnection =
                    projectInstanceWorkflowService.getProjectInstanceWorkflowConnection(
                        workflowConnection.getKey(), OptionalUtils.get(workflowConnection.getTaskName()));

                return projectInstanceWorkflowConnection.getConnectionId();
            });
    }
}
