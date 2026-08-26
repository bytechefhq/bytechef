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

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.configuration.listener.ProjectDeploymentDeleteEventListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Removes the A2A rows referencing a project deployment before that deployment is deleted, so the deployment of an A2A
 * server can be deleted together with the project backing it.
 *
 * @author Ivica Cardic
 */
@Component
public class A2aProjectDeploymentDeleteEventListener implements ProjectDeploymentDeleteEventListener {

    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;

    @SuppressFBWarnings("EI")
    public A2aProjectDeploymentDeleteEventListener(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService) {

        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
    }

    @Override
    public void onBeforeDeleteProjectDeployment(long projectDeploymentId) {
        List<A2aProject> a2aProjects = a2aProjectService.getProjectDeploymentA2aProjects(projectDeploymentId);

        for (A2aProject a2aProject : a2aProjects) {
            List<A2aProjectWorkflow> a2aProjectWorkflows = a2aProjectWorkflowService.getA2aProjectA2aProjectWorkflows(
                a2aProject.getId());

            for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflows) {
                a2aProjectWorkflowService.delete(a2aProjectWorkflow.getId());
            }

            a2aProjectService.delete(a2aProject.getId());
        }
    }
}
