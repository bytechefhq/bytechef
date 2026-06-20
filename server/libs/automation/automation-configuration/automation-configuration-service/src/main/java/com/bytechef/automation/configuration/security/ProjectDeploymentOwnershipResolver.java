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
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a project-deployment id to its owning workspace by traversing deployment &rarr; project &rarr;
 * {@code project.workspace_id}. Reads via the repositories directly (not the {@code @PreAuthorize}-guarded facade) to
 * avoid recursion. Fails closed when the deployment or its project cannot be resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectDeploymentRepository projectDeploymentRepository;
    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentOwnershipResolver(
        ProjectDeploymentRepository projectDeploymentRepository, ProjectRepository projectRepository) {

        this.projectDeploymentRepository = projectDeploymentRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeployment";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return projectDeploymentRepository.findById(id)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectRepository::findById)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
