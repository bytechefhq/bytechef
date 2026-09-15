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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a project-deployment-workflow id to the workspace owning its project for the {@code 'ProjectDeploymentWorkflow'}
 * token. Exists because that row is written by its own id: {@code ProjectDeploymentWorkflowServiceImpl.update} selects
 * by {@code getId()} and neither reads nor copies the argument's {@code projectDeploymentId}, so a guard keyed on that
 * field would be checking a deployment the write never touches — the REST path supplies the two independently. Keying
 * on the row id makes the check and the write share one handle.
 * <p>
 * Resolves through the deployment rather than joining in SQL so that only queries the rest of the tree already
 * exercises are used. Returns no {@code ownerUserId}, so CE treats deployment workflows as shared. Fails closed when
 * either hop misses.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentWorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;
    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentWorkflowOwnershipResolver(
        ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository, ProjectRepository projectRepository) {

        this.projectDeploymentWorkflowRepository = projectDeploymentWorkflowRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeploymentWorkflow";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return projectDeploymentWorkflowRepository.findById(id)
            .map(ProjectDeploymentWorkflow::getProjectDeploymentId)
            .flatMap(projectRepository::findByProjectDeploymentId)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
