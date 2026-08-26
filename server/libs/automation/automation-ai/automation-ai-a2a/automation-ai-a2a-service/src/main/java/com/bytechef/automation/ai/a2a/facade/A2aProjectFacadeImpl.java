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

package com.bytechef.automation.ai.a2a.facade;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link A2aProjectFacade}.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aProjectFacadeImpl implements A2aProjectFacade {

    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final A2aServerService a2aServerService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public A2aProjectFacadeImpl(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService,
        A2aServerService a2aServerService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.a2aServerService = a2aServerService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public A2aProject createA2aProject(
        long a2aServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {

        A2aServer a2aServer = a2aServerService.getA2aServer(a2aServerId);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName(SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX + projectId + "_v" + projectVersion);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(a2aServer.getEnvironment());
        projectDeployment.setEnabled(true);

        projectDeployment = projectDeploymentService.create(projectDeployment);

        A2aProject a2aProject = a2aProjectService.create(projectDeployment.getId(), a2aServerId);

        for (String workflowId : selectedWorkflowIds) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
                projectDeployment.getId(), workflowId);

            a2aProjectWorkflowService.create(a2aProject.getId(), projectDeploymentWorkflow.getId());
        }

        return a2aProject;
    }

    @Override
    public void deleteA2aProject(long a2aProjectId) {
        A2aProject a2aProject = a2aProjectService.fetchA2aProject(a2aProjectId)
            .orElseThrow(() -> new IllegalArgumentException("A2aProject not found: " + a2aProjectId));

        for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
            .getA2aProjectA2aProjectWorkflows(a2aProjectId)) {

            a2aProjectWorkflowService.delete(a2aProjectWorkflow.getId());

            projectDeploymentWorkflowService.delete(a2aProjectWorkflow.getProjectDeploymentWorkflowId());
        }

        a2aProjectService.delete(a2aProjectId);

        Long projectDeploymentId = a2aProject.getProjectDeploymentId();

        if (projectDeploymentId != null) {
            projectDeploymentService.delete(projectDeploymentId);
        }
    }

    @Override
    public A2aProject updateA2aProject(long a2aProjectId, List<String> selectedWorkflowIds) {
        A2aProject a2aProject = a2aProjectService.fetchA2aProject(a2aProjectId)
            .orElseThrow(() -> new IllegalArgumentException("A2aProject not found: " + a2aProjectId));

        Map<Long, ProjectDeploymentWorkflow> projectDeploymentWorkflowMap = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(a2aProject.getProjectDeploymentId())) {

            projectDeploymentWorkflowMap.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);
        }

        Map<String, A2aProjectWorkflow> existingWorkflowIdMap = new HashMap<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
            .getA2aProjectA2aProjectWorkflows(a2aProjectId)) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowMap.get(
                a2aProjectWorkflow.getProjectDeploymentWorkflowId());

            if (projectDeploymentWorkflow != null) {
                existingWorkflowIdMap.put(projectDeploymentWorkflow.getWorkflowId(), a2aProjectWorkflow);
            }
        }

        Set<String> selectedWorkflowIdSet = new HashSet<>(selectedWorkflowIds);

        for (String workflowId : selectedWorkflowIds) {
            if (!existingWorkflowIdMap.containsKey(workflowId)) {
                ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
                    a2aProject.getProjectDeploymentId(), workflowId);

                a2aProjectWorkflowService.create(a2aProjectId, projectDeploymentWorkflow.getId());
            }
        }

        for (Map.Entry<String, A2aProjectWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                A2aProjectWorkflow a2aProjectWorkflow = entry.getValue();

                a2aProjectWorkflowService.delete(a2aProjectWorkflow.getId());

                projectDeploymentWorkflowService.delete(a2aProjectWorkflow.getProjectDeploymentWorkflowId());
            }
        }

        return a2aProject;
    }

    private ProjectDeploymentWorkflow createProjectDeploymentWorkflow(long projectDeploymentId, String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(Map.of());

        return projectDeploymentWorkflowService.create(projectDeploymentWorkflow);
    }
}
