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

import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ProjectSearchAssetProvider implements SearchAssetProvider {

    private final ProjectService projectService;

    ProjectSearchAssetProvider(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public List<ProjectSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        return projectService.getProjects(false, null, null, null, null, null)
            .stream()
            .filter(
                project -> containsIgnoreCase(project.getName(), queryLower) ||
                    containsIgnoreCase(project.getDescription(), queryLower))
            .limit(limit)
            .map(project -> new ProjectSearchResult(project.getId(), project.getName(), project.getDescription()))
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.PROJECT;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
