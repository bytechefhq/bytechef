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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectTagFacadeImpl implements ProjectTagFacade {

    private final ProjectService projectService;
    private final ProjectVisibilityFilter projectVisibilityFilter;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectTagFacadeImpl(
        ProjectService projectService, ProjectVisibilityFilter projectVisibilityFilter, TagService tagService) {

        this.projectService = projectService;
        this.projectVisibilityFilter = projectVisibilityFilter;
        this.tagService = tagService;
    }

    /**
     * Filtered through {@link ProjectVisibilityFilter} for the same reason every project listing is: this feeds the tag
     * dropdown over the project list, so a tag aggregated off a project the caller cannot see is both a name disclosed
     * from a withheld project and a filter option that selects nothing. One batched call over the projects already
     * loaded, not a per-row check.
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public List<Tag> getProjectTags(long workspaceId) {
        List<Long> projectIds = projectService.getWorkspaceProjectIds(workspaceId);

        List<Project> projects = projectVisibilityFilter.filterVisible(projectService.getProjects(projectIds));

        return tagService.getTags(CollectionUtils.flatMap(projects, Project::getTagIds));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_EDIT')")
    public void updateProjectTags(long id, List<Tag> tags) {
        tags = checkTags(tags);

        projectService.update(id, CollectionUtils.map(tags, Tag::getId));
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }
}
