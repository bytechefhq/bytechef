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
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * A workflow has no visibility of its own: its reach IS its project's, resolved here at check time (resource-visibility
 * spec §11). Workflow ids are strings, so the {@link Serializable} overload is the real entry point and the numeric one
 * fails closed.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public WorkflowVisibilityProvider(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "Workflow";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(Serializable id) {
        if (!(id instanceof String workflowId)) {
            return Optional.empty();
        }

        return projectRepository.findByWorkflowId(workflowId)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
