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
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
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
    private final TagService tagService;

    @SuppressFBWarnings("EI")
    public ProjectGraphQlController(
        CategoryService categoryService, ProjectFacade projectFacade, TagService tagService) {

        this.categoryService = categoryService;
        this.projectFacade = projectFacade;
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

    @MutationMapping(name = "updateProjectErrorWorkflow")
    public Boolean updateProjectErrorWorkflow(
        @Argument long projectId, @Argument @Nullable Long errorProjectWorkflowId) {

        try {
            projectFacade.updateProjectErrorWorkflow(projectId, errorProjectWorkflowId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new GraphQlBadRequestException(illegalArgumentException.getMessage(), illegalArgumentException);
        }

        return true;
    }

    /**
     * The project behind the {@code project} query.
     *
     * <p>
     * Authorization lives on {@link ProjectFacade#getProjectRow(long)}, which carries
     * {@code hasPermission(#id, 'Project', 'WORKFLOW_VIEW')} — the same visibility-preconditioned scope check this
     * method used to run in its own body, for want of a facade method returning the entity the {@code Project} field
     * resolvers need.
     */
    @QueryMapping(name = "project")
    public Project project(@Argument long id) {
        return projectFacade.getProjectRow(id);
    }

    /**
     * Every project the caller may open, and no more.
     *
     * <p>
     * Narrowing lives on {@link ProjectFacade#getProjectRows()}, which keeps the caller to the workspaces they hold
     * {@code WORKFLOW_VIEW} in and then filters through {@code ProjectVisibilityFilter} in one batched call — the two
     * halves {@code hasResourceScope(id, 'Project', scope)} composes for a project. Until that seam existed this method
     * assembled the listing itself out of {@code ProjectService} and {@code PermissionService}, past the facade layer
     * that owns authorization.
     *
     * <p>
     * There is no {@code @PreAuthorize} on that facade method to inherit, and there should not be — a tenant-wide
     * listing has no id to gate on, so it is filtered rather than guarded. The surface audit exempts both ends saying
     * exactly that, rather than pointing at an annotation that would overstate what it does.
     */
    @QueryMapping(name = "projects")
    public List<Project> projects() {
        return projectFacade.getProjectRows();
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
