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

package com.bytechef.automation.configuration.instance.accessor;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentJobPrincipalAccessor implements JobPrincipalAccessor {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentJobPrincipalAccessor(
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
    public boolean isWorkflowEnabled(long jobPrincipalId, String workflowUuid) {
        boolean workflowEnabled = false;

        if (projectDeploymentService.isProjectDeploymentEnabled(jobPrincipalId) &&
            projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
                jobPrincipalId, getWorkflowId(jobPrincipalId, workflowUuid))) {

            workflowEnabled = true;
        }

        return workflowEnabled;
    }

    @Override
    public long getEnvironmentId(long jobPrincipalId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);

        Environment environment = projectDeployment.getEnvironment();

        return environment.ordinal();
    }

    @Override
    public Map<String, ?> getInputMap(long jobPrincipalId, String workflowUuid) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
                jobPrincipalId, getWorkflowId(jobPrincipalId, workflowUuid));

        return projectDeploymentWorkflow.getInputs();
    }

    @Override
    public Map<String, ?> getMetadataMap(long jobPrincipalId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);

        return Map.of("projectVersion", projectDeployment.getProjectVersion());
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    public String getWorkflowId(long jobPrincipalId, String workflowUuid) {
        return projectWorkflowService.getProjectDeploymentWorkflowId(jobPrincipalId, workflowUuid);
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        return projectWorkflowService.getLastWorkflowId(workflowUuid);
    }

    @Override
    public String getWorkflowUuid(String workflowId) {
        ProjectWorkflow workflowProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return workflowProjectWorkflow.getUuidAsString();
    }
}
