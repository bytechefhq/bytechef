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

package com.bytechef.automation.configuration.search;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ProjectDeploymentSearchAssetProvider implements SearchAssetProvider {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    ProjectDeploymentSearchAssetProvider(ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public List<ProjectDeploymentSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        List<ProjectDeployment> deployments = projectDeploymentService.getProjectDeployments();

        if (deployments.isEmpty()) {
            return List.of();
        }

        List<Long> projectIds = deployments.stream()
            .map(ProjectDeployment::getProjectId)
            .distinct()
            .toList();

        Map<Long, Project> projectMap = projectService.getProjects(projectIds)
            .stream()
            .collect(Collectors.toMap(Project::getId, Function.identity()));

        return deployments.stream()
            .filter(deployment -> {
                Project project = projectMap.get(deployment.getProjectId());

                return project != null && containsIgnoreCase(project.getName(), queryLower);
            })
            .limit(limit)
            .map(deployment -> {
                Project project = projectMap.get(deployment.getProjectId());
                String projectName = project != null ? project.getName() : "Unknown";

                return new ProjectDeploymentSearchResult(deployment.getId(), projectName);
            })
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.DEPLOYMENT;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
