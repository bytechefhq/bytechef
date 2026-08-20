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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * A deployment has no visibility of its own: it is exactly as visible as the project it deploys, so the record returned
 * here is the PROJECT's and its grants are looked up under {@code "Project"}.
 *
 * <p>
 * This governs the MANAGEMENT surfaces only. Hiding a project does not undeploy it — its webhooks, schedules and
 * triggers keep serving traffic, because no runtime path consults visibility.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectDeploymentVisibilityProvider implements ResourceVisibilityProvider {

    private final ProjectDeploymentRepository projectDeploymentRepository;
    private final ProjectRepository projectRepository;

    @SuppressFBWarnings("EI")
    public ProjectDeploymentVisibilityProvider(
        ProjectDeploymentRepository projectDeploymentRepository, ProjectRepository projectRepository) {

        this.projectDeploymentRepository = projectDeploymentRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public String resourceType() {
        return "ProjectDeployment";
    }

    @Override
    public String visibilityResourceType() {
        return ProjectVisibilityFilter.PROJECT;
    }

    @Override
    public Optional<VisibilityRecord> fetchVisibility(long id) {
        return projectDeploymentRepository.findById(id)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectRepository::findById)
            .map(ProjectVisibilityFilter::toVisibilityRecord);
    }
}
