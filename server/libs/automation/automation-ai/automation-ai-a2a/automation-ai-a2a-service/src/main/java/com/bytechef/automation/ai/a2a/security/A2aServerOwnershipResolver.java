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

package com.bytechef.automation.ai.a2a.security;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aProjectRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class A2aServerOwnershipResolver implements ResourceOwnershipResolver {

    private final A2aProjectRepository a2aProjectRepository;
    private final A2aServerRepository a2aServerRepository;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public A2aServerOwnershipResolver(
        A2aProjectRepository a2aProjectRepository, A2aServerRepository a2aServerRepository,
        ProjectService projectService) {

        this.a2aProjectRepository = a2aProjectRepository;
        this.a2aServerRepository = a2aServerRepository;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "A2aServer";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return a2aServerRepository.findById(id)
            .map(A2aServer::getWorkspaceId)
            .filter(workspaceId -> hasOnlyProjectsOfWorkspace(id, workspaceId))
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }

    private boolean hasOnlyProjectsOfWorkspace(long a2aServerId, long workspaceId) {
        List<Long> projectIds = a2aProjectRepository.findAllByA2aServerId(a2aServerId)
            .stream()
            .map(A2aProject::getProjectId)
            .distinct()
            .toList();

        if (projectIds.isEmpty()) {
            return true;
        }

        List<Project> projects = projectService.getProjects(projectIds);

        return projects.size() == projectIds.size() && projects.stream()
            .allMatch(project -> Objects.equals(project.getWorkspaceId(), workspaceId));
    }
}
