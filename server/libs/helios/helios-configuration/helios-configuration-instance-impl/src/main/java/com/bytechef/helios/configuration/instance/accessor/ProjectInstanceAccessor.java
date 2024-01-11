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

package com.bytechef.helios.configuration.instance.accessor;

import com.bytechef.helios.configuration.domain.ProjectInstance;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.hermes.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceAccessor implements InstanceAccessor {

    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceAccessor(
        ProjectInstanceService projectInstanceService, ProjectInstanceWorkflowService projectInstanceWorkflowService) {

        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(
        long connectionId, String workflowId, String workflowConnectionOperationName, String workflowConnectionKey) {

        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances();

        for (ProjectInstance projectInstance : projectInstances) {
            if (projectInstanceWorkflowService.fetchProjectInstanceWorkflowConnection(
                Validate.notNull(projectInstance.getId(), "id"), workflowId, workflowConnectionOperationName,
                workflowConnectionKey)
                .map(ProjectInstanceWorkflowConnection::getConnectionId)
                .map(curConnectionId -> Objects.equals(curConnectionId, connectionId))
                .orElse(false)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isWorkflowEnabled(long instanceId, String workflowId) {
        boolean workflowEnabled = false;

        if (projectInstanceService.isProjectInstanceEnabled(instanceId) &&
            projectInstanceWorkflowService.isProjectInstanceWorkflowEnabled(instanceId, workflowId)) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public Map<String, ?> getInputMap(long instanceId, String workflowId) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            instanceId, workflowId);

        return projectInstanceWorkflow.getInputs();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }
}
