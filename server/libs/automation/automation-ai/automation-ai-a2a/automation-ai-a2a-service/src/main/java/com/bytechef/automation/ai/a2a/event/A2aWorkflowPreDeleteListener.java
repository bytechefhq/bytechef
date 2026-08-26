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

package com.bytechef.automation.ai.a2a.event;

import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Removes the A2A rows referencing a workflow, together with the project deployment workflow rows they point at, before
 * that workflow is deleted.
 * <p>
 * An A2A server is backed by a synthetic project deployment which the deployments list deliberately hides, so the
 * project deployment workflow cleanup performed by the workflow delete itself never sees it. Only the project
 * deployment workflows carrying A2A rows are removed here, which keeps this listener from touching rows owned by
 * another feature or by a regular deployment.
 *
 * @author Ivica Cardic
 */
@Component
public class A2aWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(A2aWorkflowPreDeleteListener.class);

    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public A2aWorkflowPreDeleteListener(
        A2aProjectWorkflowService a2aProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        log.debug("Cleaning up A2A data for workflow {}", workflowId);

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows(workflowId);

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            long projectDeploymentWorkflowId = projectDeploymentWorkflow.getId();

            List<A2aProjectWorkflow> a2aProjectWorkflows =
                a2aProjectWorkflowService.getProjectDeploymentWorkflowA2aProjectWorkflows(projectDeploymentWorkflowId);

            if (a2aProjectWorkflows.isEmpty()) {
                continue;
            }

            for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflows) {
                a2aProjectWorkflowService.delete(a2aProjectWorkflow.getId());
            }

            projectDeploymentWorkflowService.delete(projectDeploymentWorkflowId);
        }
    }
}
