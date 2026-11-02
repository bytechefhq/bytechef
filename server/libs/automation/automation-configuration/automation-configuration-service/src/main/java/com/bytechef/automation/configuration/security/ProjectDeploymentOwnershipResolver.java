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
import com.bytechef.automation.configuration.repository.ProjectRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps a project-deployment id to the workspace owning its project for the {@code 'ProjectDeployment'} token. Reads the
 * repository directly (not the {@code @PreAuthorize}-guarded facade) to avoid recursion, and joins through to the
 * project because a deployment carries no workspace of its own. Returns no {@code ownerUserId}, so CE treats
 * deployments as shared. Fails closed when the deployment cannot be resolved.
 * <p>
 * Without this resolver {@code PermissionService.hasResourceScope} denies every non-tenant-admin for the
 * {@code 'ProjectDeployment'} token — the registry lookup fails closed — which would make a gate naming that token a
 * lockout rather than a check. Registering it also brings {@link ProjectDeploymentEnvironmentResolver} into play, so a
 * by-id deployment check is answered by the role the caller holds in the deployment's own environment rather than in
 * any environment they can reach.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentOwnershipResolver(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeployment";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return projectRepository.findByProjectDeploymentId(id)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
