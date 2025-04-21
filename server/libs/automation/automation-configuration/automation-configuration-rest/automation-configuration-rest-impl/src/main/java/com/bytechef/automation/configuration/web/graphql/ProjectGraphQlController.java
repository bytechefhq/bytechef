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
    private final ProjectService projectService;
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectGraphQlController(
        CategoryService categoryService, ProjectService projectService, TagService tagService) {

        this.categoryService = categoryService;
        this.projectService = projectService;
        this.tagService = tagService;
    }

    @SchemaMapping
    public Category category(Project project) {
        if (project.getCategoryId() == null) {
            return null;
        }

        return categoryService.getCategory(project.getCategoryId());
    }

    @QueryMapping
    public Project project(@Argument long id) {
        return projectService.getProject(id);
    }

    @QueryMapping
    public List<Project> projects() {
        return projectService.getProjects();
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
