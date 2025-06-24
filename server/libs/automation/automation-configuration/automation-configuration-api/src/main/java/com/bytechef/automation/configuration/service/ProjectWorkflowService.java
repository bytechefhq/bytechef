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

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ProjectWorkflowService {

    ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId);

    ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId, String workflowReferenceCode);

    void delete(List<Long> ids);

    void delete(long projectId, int projectVersion, String workflowId);

    Optional<String> fetchLatestProjectWorkflowId(Long projectId, String workflowReferenceCode);

    Optional<ProjectWorkflow> fetchProjectWorkflow(long projectId, int projectVersion, String workflowReferenceCode);

    ProjectWorkflow getLatestProjectWorkflow(Long projectId, String workflowReferenceCode);

    String getLatestWorkflowId(String workflowReferenceCode);

    List<Long> getProjectProjectWorkflowIds(long projectId, int projectVersion);

    ProjectWorkflow getProjectWorkflow(long id);

    List<String> getProjectWorkflowIds(long projectId);

    List<String> getProjectWorkflowIds(long projectId, int projectVersion);

    List<ProjectWorkflow> getProjectWorkflows();

    List<ProjectWorkflow> getProjectWorkflows(long projectId);

    List<ProjectWorkflow> getProjectWorkflows(long projectId, int projectVersion);

    List<ProjectWorkflow> getProjectWorkflows(Long projectId, String workflowReferenceCode);

    String getProjectDeploymentWorkflowId(long projectDeploymentId, String workflowReferenceCode);

    String getProjectDeploymentWorkflowReferenceCode(long projectDeploymentId, String workflowId);

    ProjectWorkflow getWorkflowProjectWorkflow(String workflowId);

    ProjectWorkflow update(ProjectWorkflow projectWorkflow);

}
