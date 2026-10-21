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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.dto.DeployProjectResultDTO;
import com.bytechef.automation.configuration.dto.DeploymentConnectionChoiceDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ProjectDeploymentFacade {

    long createProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO);

    long createProjectDeployment(
        ProjectDeployment projectDeployment, String workflowId, List<ProjectDeploymentWorkflowConnection> connections);

    long createProjectDeployment(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows,
        List<Tag> tags);

    long createProjectDeploymentWorkflowJob(Long id, String workflowId);

    void deleteProjectDeployment(long id);

    /**
     * Creates or updates the project's single DEVELOPMENT deployment at its latest published version, leaving it
     * disabled; the caller arms it with {@link #enableProjectDeployment(long, boolean)}. Workflows are named by uuid.
     * Every workflow of the version gets a row: listed uuids are enabled, the others keep their current state and a new
     * row starts disabled. Connections and inputs not given keep the current row's value; a slot with exactly one
     * usable candidate gets it. Nothing is written when an enabled workflow has an unresolved required slot or a
     * missing required input; the result lists them.
     */
    DeployProjectResultDTO deployProject(
        long projectId, @Nullable Long projectDeploymentId, List<String> enabledWorkflowUuids,
        List<DeploymentConnectionChoiceDTO> connections, Map<String, Map<String, ?>> inputs);

    void enableProjectDeployment(long id, boolean enable);

    void enableProjectDeploymentWorkflow(long projectDeploymentId, String workflowId, boolean enable);

    void enableProjectDeploymentWorkflow(long projectId, String workflowId, boolean enable, Environment environment);

    Set<Long> getOwnedProjectDeploymentIds(
        Collection<Long> projectDeploymentIds, ProjectDeploymentOwner projectDeploymentOwner);

    ProjectDeploymentDTO getProjectDeployment(long id);

    /**
     * Returns the project deployments of a workspace in one environment, optionally narrowed to a project or a tag,
     * leaving out the deployments a feature owns, with no related data joined.
     */
    List<ProjectDeployment> getProjectDeployments(
        long workspaceId, Environment environment, @Nullable Long projectId, @Nullable Long tagId);

    /**
     * Returns the deployment workflow of the given project deployment that runs the workflow identified by its UUID.
     */
    ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentId, String workflowUuid);

    List<Tag> getProjectDeploymentTags(long id, Long environmentId);

    List<ProjectDeploymentDTO> getWorkspaceProjectDeployments(
        long id, Long environmentId, Long projectId, Long tagId, boolean includeAllFields);

    void updateProjectDeployment(ProjectDeploymentDTO projectDeploymentDTO);

    void updateProjectDeployment(
        long projectId, int projectVersion, String workflowUuid,
        List<ProjectDeploymentWorkflowConnection> connections, Long environmentId);

    void updateProjectDeployment(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows,
        List<Tag> tags);

    void updateProjectDeploymentTags(long id, List<Tag> tags);

    void updateProjectDeploymentWorkflow(ProjectDeploymentWorkflow projectDeploymentWorkflow);
}
