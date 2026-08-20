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
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Registers projects with the visibility precondition of {@code PermissionService.hasResourceScope}. Reads the
 * repository directly (not the guarded facade) to avoid recursion.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectVisibilityProvider(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return projectRepository.findById(id)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
