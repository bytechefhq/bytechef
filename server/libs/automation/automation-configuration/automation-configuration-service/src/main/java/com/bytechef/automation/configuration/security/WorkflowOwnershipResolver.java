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
import java.io.Serializable;
import org.springframework.stereotype.Component;

/**
 * Maps a workflow id (a String UUID) to its owning workspace via {@code workflow -> project -> project.workspace_id}
 * for the {@code 'Workflow'} token. Overrides the {@link Serializable} resolver because workflow ids are String-keyed,
 * not numeric — the numeric {@link #resolveOwner(long)} is unreachable for workflows and fails closed. Returns no
 * {@code ownerUserId} so CE treats workflows as shared (permissive). Fails closed when the workflow cannot be resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public WorkflowOwnershipResolver(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "Workflow";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return ResourceOwner.unknown();
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        if (!(id instanceof String workflowId)) {
            return ResourceOwner.unknown();
        }

        return projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
