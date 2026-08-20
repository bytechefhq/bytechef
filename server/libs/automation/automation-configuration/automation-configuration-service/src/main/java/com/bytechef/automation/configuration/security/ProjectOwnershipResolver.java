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

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps a project id to its owning workspace ({@code project.workspace_id}) and, via {@code created_by}, its owner user
 * id (needed by {@code isResourceOwner('Project', …)} on the sharing facade). Reads the repository directly (not the
 * {@code @PreAuthorize}-guarded facade) to avoid recursion. Fails closed when the project cannot be resolved.
 *
 * <p>
 * CE {@code hasResourceScope} does not owner-isolate projects because {@link ProjectVisibilityProvider} is registered —
 * visibility decides instead; the two must be registered together. Returning an owner here without that provider would
 * hide every project from everyone but its creator.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectOwnershipResolver implements ResourceOwnershipResolver {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ProjectOwnershipResolver(ProjectRepository projectRepository, UserService userService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
    }

    @Override
    public String resourceType() {
        return "Project";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return projectRepository.findById(id)
            .map(project -> ResourceOwner.of(
                toOptionalLong(project.getWorkspaceId()), resolveOwnerUserId(project.getCreatedBy())))
            .orElseGet(ResourceOwner::unknown);
    }

    /**
     * Resolved on every call, not only for a project the caller cannot already see: an owner must be able to change the
     * visibility of a project that is currently WORKSPACE-visible, so {@code isResourceOwner('Project', …)} needs this
     * regardless of reach. The lookup is a third query on the authorization path, but a cached one —
     * {@code UserRepository.findByLogin} is {@code @Cacheable(USERS_BY_LOGIN_CACHE)} and tenant-keyed, evicted by
     * {@code UserServiceImpl} on every user mutation.
     */
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
