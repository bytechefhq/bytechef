
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.demo.config;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Configuration
public class OrphanWorkflowsLoadConfiguration implements InitializingBean {

    private static final String WORKFLOWS = "Workflows";

    private final CategoryService categoryService;
    private final ProjectService projectService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public OrphanWorkflowsLoadConfiguration(
        CategoryService categoryService, ProjectService projectService, TagService tagService,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.projectService = projectService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void afterPropertiesSet() {
        List<Project> projects = projectService.getProjects();
        List<Workflow> workflows = workflowService.getWorkflows();

        List<String> projectWorkflowIds = CollectionUtils.map(projects, Project::getWorkflowIds)
            .stream()
            .flatMap(Collection::stream)
            .toList();

        List<Workflow> orphanWorkflows = new ArrayList<>();

        for (Workflow workflow : workflows) {
            String workflowId = workflow.getId();

            if (!CollectionUtils.contains(projectWorkflowIds, workflowId)) {
                orphanWorkflows.add(workflow);
            }
        }

        if (!orphanWorkflows.isEmpty()) {
            for (Workflow workflow : orphanWorkflows) {
                Long categoryId;
                String projectName;

                if (StringUtils.hasText((String) workflow.getMetadata(WorkflowConstants.PATH))) {
                    String path = (String) workflow.getMetadata(WorkflowConstants.PATH);

                    String[] items = path.split("/");

                    if (items.length > 2) {
                        String categoryName = StringUtils.capitalize(items[items.length - 3]);

                        Category category = categoryService.save(new Category(categoryName));

                        categoryId = category.getId();

                        projectName = categoryName + " " + getProjectName(items[items.length - 2]);
                    } else if (items.length > 1) {
                        categoryId = null;
                        projectName = getProjectName(items[items.length - 2]);
                    } else {
                        categoryId = null;
                        projectName = WORKFLOWS;
                    }
                } else {
                    categoryId = null;
                    projectName = WORKFLOWS;
                }

                Project project = projectService.fetchProject(projectName)
                    .orElseGet(() -> projectService.create(
                        Project.builder()
                            .categoryId(categoryId)
                            .name(projectName)
                            .status(Project.Status.PUBLISHED)
                            .tagIds(getTagIds(workflow.getSourceType()))
                            .build()));

                projectService.addWorkflow(Objects.requireNonNull(project.getId()), workflow.getId());
            }
        }
    }

    private String getProjectName(String name) {
        return Arrays.stream(name.split("_"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    private List<Long> getTagIds(Workflow.SourceType sourceType) {
        String sourceTypeName = sourceType.name();

        return tagService.save(List.of(new Tag(sourceTypeName.toLowerCase())))
            .stream()
            .map(Tag::getId)
            .toList();
    }
}
