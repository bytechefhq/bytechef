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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a project-workflow row id to its owning workspace by traversing project workflow &rarr; project &rarr;
 * {@code project.workspace_id}. Reads via the repositories directly (not the {@code @PreAuthorize}-guarded facade) to
 * avoid recursion. Fails closed when the row or its project cannot be resolved.
 * <p>
 * Without this resolver, {@code hasPermission(#id, 'ProjectWorkflow', ...)} would resolve to no resolver at all and
 * {@code PermissionServiceImpl.hasResourceScope} returns false for an unknown resource type -- denying every caller
 * except tenant admins, which reads as a broken feature rather than a missing registration.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectWorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectRepository projectRepository;
    private final ProjectWorkflowRepository projectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowOwnershipResolver(
        ProjectRepository projectRepository, ProjectWorkflowRepository projectWorkflowRepository) {

        this.projectRepository = projectRepository;
        this.projectWorkflowRepository = projectWorkflowRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectWorkflow";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return projectWorkflowRepository.findById(id)
            .map(ProjectWorkflow::getProjectId)
            .flatMap(projectRepository::findById)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
