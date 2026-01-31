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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
public interface ProjectDeploymentWorkflowService {

    ProjectDeploymentWorkflow create(ProjectDeploymentWorkflow projectDeploymentWorkflow);

    List<ProjectDeploymentWorkflow> create(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows);

    void delete(long id);

    void deleteProjectDeploymentWorkflowConnection(long connectionId);

    @Transactional(readOnly = true)
    Optional<ProjectDeploymentWorkflow> fetchProjectDeploymentWorkflow(long projectDeploymentId, String workflowId);

    Optional<ProjectDeploymentWorkflowConnection> fetchProjectDeploymentWorkflowConnection(
        long projectDeploymentOd, String workflowId, String workflowNodeName, String workflowConnectionKey);

    ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentWorkflowId);

    ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentId, String workflowId);

    List<ProjectDeploymentWorkflowConnection> getProjectDeploymentWorkflowConnections(
        long projectDeploymentId, String workflowId, String workflowNodeName);

    List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(long projectDeploymentId);

    List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(List<Long> projectDeploymentIds);

    boolean isConnectionUsed(long connectionId);

    boolean isProjectDeploymentWorkflowEnabled(long projectDeploymentId, String workflowId);

    ProjectDeploymentWorkflow update(ProjectDeploymentWorkflow projectDeploymentWorkflow);

    List<ProjectDeploymentWorkflow> update(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows);

    void updateEnabled(Long id, boolean enable);
}
