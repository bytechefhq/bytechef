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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ProjectGraphQlController {

    private final CategoryService categoryService;
    private final ProjectFacade projectFacade;
    private final ProjectService projectService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectGraphQlController(
        CategoryService categoryService, ProjectFacade projectFacade, ProjectService projectService,
        TagService tagService) {

        this.categoryService = categoryService;
        this.projectFacade = projectFacade;
        this.projectService = projectService;
        this.tagService = tagService;
    }

    @SchemaMapping(typeName = "Project", field = "category")
    public Category category(Project project) {
        if (project.getCategoryId() == null) {
            return null;
        }

        return categoryService.getCategory(project.getCategoryId());
    }

    @MutationMapping(name = "deleteSharedProject")
    public Boolean deleteSharedProject(@Argument Long id) {
        projectFacade.deleteSharedProject(id);

        return true;
    }

    @MutationMapping(name = "exportSharedProject")
    public void exportSharedProject(@Argument Long id, @Argument("description") String description) {
        projectFacade.exportSharedProject(id, description);
    }

    @QueryMapping(name = "projectTemplate")
    public ProjectTemplateDTO projectTemplate(@Argument String id, @Argument boolean sharedProject) {
        return projectFacade.getProjectTemplate(id, sharedProject);
    }

    @QueryMapping(name = "preBuiltProjectTemplates")
    public List<ProjectTemplateDTO> preBuiltProjectTemplates(@Argument String query, @Argument String category) {
        return projectFacade.getPreBuiltProjectTemplates(query, category);
    }

    @MutationMapping(name = "importProjectTemplate")
    public Long importProjectTemplate(
        @Argument String id, @Argument Long workspaceId, @Argument boolean sharedProject) {

        return projectFacade.importProjectTemplate(id, workspaceId, sharedProject);
    }

    @QueryMapping(name = "project")
    public Project project(@Argument long id) {
        return projectService.getProject(id);
    }

    @QueryMapping(name = "projects")
    public List<Project> projects() {
        return projectService.getProjects();
    }

    @QueryMapping(name = "sharedProject")
    public SharedProjectDTO sharedProject(@Argument String projectUuid) {
        return projectFacade.getSharedProject(projectUuid);
    }

    @BatchMapping
    public Map<Project, List<Tag>> tags(List<Project> projects) {
        var tagIds = projects.stream()
            .flatMap(project -> CollectionUtils.stream(project.getTagIds()))
            .toList();

        List<Tag> tags = tagService.getTags(tagIds);

        return projects.stream()
            .collect(
                Collectors.toMap(
                    project -> project,
                    project -> tags.stream()
                        .filter(tag -> {
                            List<Long> curTagIds = project.getTagIds();

                            return curTagIds.contains(tag.getId());
                        })
                        .toList()));
    }
}
