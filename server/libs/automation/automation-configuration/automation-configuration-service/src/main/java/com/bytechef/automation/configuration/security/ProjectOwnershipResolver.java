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
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Maps a project id to its owning workspace ({@code project.workspace_id}) and, via {@code created_by}, its owner user
 * id. Reads the repository directly (not the {@code @PreAuthorize}-guarded facade) to avoid recursion. Fails closed
 * when the project cannot be resolved.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectOwnershipResolver implements ResourceOwnershipResolver {

    private final ObjectProvider<EmbeddedResourceResolver> embeddedResourceResolverProvider;
    private final ProjectRepository projectRepository;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ProjectOwnershipResolver(
        ObjectProvider<EmbeddedResourceResolver> embeddedResourceResolverProvider, ProjectRepository projectRepository,
        UserService userService) {

        this.embeddedResourceResolverProvider = embeddedResourceResolverProvider;
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "Project";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        EmbeddedResourceResolver embeddedResourceResolver = embeddedResourceResolverProvider.getIfAvailable();

        if (embeddedResourceResolver != null && embeddedResourceResolver.isEmbeddedProject(id)) {
            return ResourceOwner.ofEmbeddedProject();
        }

        return fetchProject(id)
            .map(project -> ResourceOwner.of(
                toOptionalLong(project.getWorkspaceId()), resolveOwnerUserId(project.getCreatedBy())))
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public OptionalLong resolveProjectId(Serializable id) {
        if (!(id instanceof Number number)) {
            return OptionalLong.empty();
        }

        return fetchProject(number.longValue())
            .map(project -> OptionalLong.of(project.getId()))
            .orElseGet(OptionalLong::empty);
    }

    private Optional<Project> fetchProject(long id) {
        return projectRepository.findById(id);
    }

    private OptionalLong resolveOwnerUserId(String createdBy) {
        if (createdBy == null) {
            return OptionalLong.empty();
        }

        return userService.fetchUserByLogin(createdBy)
            .map(user -> toOptionalLong(user.getId()))
            .orElseGet(OptionalLong::empty);
    }

    private static OptionalLong toOptionalLong(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }
}
