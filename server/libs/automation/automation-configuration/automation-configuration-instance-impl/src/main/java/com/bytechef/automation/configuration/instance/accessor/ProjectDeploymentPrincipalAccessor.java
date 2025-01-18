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

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.instance.accessor.PrincipalAccessor;
import com.bytechef.platform.constant.ModeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentPrincipalAccessor implements PrincipalAccessor {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentPrincipalAccessor(
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return projectDeploymentWorkflowService.isConnectionUsed(connectionId);
    }

    @Override
    public boolean isWorkflowEnabled(long principalId, String workflowReferenceCode) {
        boolean workflowEnabled = false;

        if (projectDeploymentService.isProjectDeploymentEnabled(principalId) &&
            projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
                principalId, getWorkflowId(principalId, workflowReferenceCode))) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public Map<String, ?> getInputMap(long principalId, String workflowReferenceCode) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                principalId, getWorkflowId(principalId, workflowReferenceCode));

        return projectDeploymentWorkflow.getInputs();
    }

    @Override
    public ModeType getType() {
        return ModeType.AUTOMATION;
    }

    @Override
    public String getWorkflowId(long principalId, String workflowReferenceCode) {
        return projectWorkflowService.getProjectDeploymentProjectWorkflowWorkflowId(principalId, workflowReferenceCode);
    }
}
