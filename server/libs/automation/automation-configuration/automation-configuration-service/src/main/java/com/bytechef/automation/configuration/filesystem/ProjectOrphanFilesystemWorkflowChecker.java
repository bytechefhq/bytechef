/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.automation.configuration.filesystem;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.annotation.ConditionalOnWorkflowRepositoryFilesystem;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.constant.Type;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCoordinator
@ConditionalOnWorkflowRepositoryFilesystem
public class ProjectOrphanFilesystemWorkflowChecker {

    private static final String WORKFLOWS = "Workflows";

    private final ProjectService projectService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectOrphanFilesystemWorkflowChecker(
        ProjectService projectService, TagService tagService, WorkflowService workflowService) {

        this.projectService = projectService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReadyEvent() {
        List<Project> projects = projectService.getProjects();
        List<Workflow> workflows = workflowService.getWorkflows(Type.AUTOMATION.ordinal());

        List<Workflow> orphanWorkflows = new ArrayList<>();
        List<String> projectWorkflowIds = CollectionUtils.flatMap(
            projects, project -> project.getAllWorkflowIds()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList());

        for (Workflow workflow : workflows) {
            String workflowId = workflow.getId();

            if (!CollectionUtils.contains(projectWorkflowIds, workflowId)) {
                orphanWorkflows.add(workflow);
            }
        }

        if (!orphanWorkflows.isEmpty()) {
            for (Workflow workflow : orphanWorkflows) {
                String projectName;

                if (StringUtils.isNotBlank((String) workflow.getMetadata(WorkflowConstants.PATH))) {
                    String path = (String) workflow.getMetadata(WorkflowConstants.PATH);

                    String[] items = path.split("/");

                    if (items.length > 1) {
                        projectName = getProjectName(items[items.length - 2]);
                    } else {
                        projectName = WORKFLOWS;
                    }
                } else {
                    projectName = WORKFLOWS;
                }

                Project project = projectService.fetchProject(projectName)
                    .orElseGet(() -> projectService.create(
                        Project.builder()
                            .name(projectName)
                            .tagIds(getTagIds(workflow.getSourceType()))
                            .build()));

                projectService.addWorkflow(Validate.notNull(project.getId(), "id"), workflow.getId());
            }
        }
    }

    private String getProjectName(String name) {
        return Arrays.stream(name.split("_"))
            .map(org.springframework.util.StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    private List<Long> getTagIds(Workflow.SourceType sourceType) {
        String sourceTypeName = sourceType.name();

        return CollectionUtils.map(tagService.save(List.of(new Tag(sourceTypeName.toLowerCase()))), Tag::getId);
    }
}
