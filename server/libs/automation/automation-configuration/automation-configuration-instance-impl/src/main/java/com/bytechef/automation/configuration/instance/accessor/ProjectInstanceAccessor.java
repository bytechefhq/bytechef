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

package com.bytechef.automation.configuration.instance.accessor;

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.service.ProjectInstanceService;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.instance.accessor.InstanceAccessor;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectInstanceAccessor implements InstanceAccessor {

    private final ProjectInstanceService projectInstanceService;
    private final ProjectInstanceWorkflowService projectInstanceWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectInstanceAccessor(
        ProjectInstanceService projectInstanceService, ProjectInstanceWorkflowService projectInstanceWorkflowService,
        ProjectWorkflowService projectWorkflowService) {

        this.projectInstanceService = projectInstanceService;
        this.projectInstanceWorkflowService = projectInstanceWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return projectInstanceWorkflowService.isConnectionUsed(connectionId);
    }

    @Override
    public boolean isWorkflowEnabled(long instanceId, String workflowReferenceCode) {
        boolean workflowEnabled = false;

        if (projectInstanceService.isProjectInstanceEnabled(instanceId) &&
            projectInstanceWorkflowService.isProjectInstanceWorkflowEnabled(
                instanceId, getWorkflowId(instanceId, workflowReferenceCode))) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public Map<String, ?> getInputMap(long instanceId, String workflowReferenceCode) {
        ProjectInstanceWorkflow projectInstanceWorkflow = projectInstanceWorkflowService.getProjectInstanceWorkflow(
            instanceId, getWorkflowId(instanceId, workflowReferenceCode));

        return projectInstanceWorkflow.getInputs();
    }

    @Override
    public AppType getType() {
        return AppType.AUTOMATION;
    }

    @Override
    public String getWorkflowId(long instanceId, String workflowReferenceCode) {
        return projectWorkflowService.getProjectWorkflowId(instanceId, workflowReferenceCode);
    }
}
