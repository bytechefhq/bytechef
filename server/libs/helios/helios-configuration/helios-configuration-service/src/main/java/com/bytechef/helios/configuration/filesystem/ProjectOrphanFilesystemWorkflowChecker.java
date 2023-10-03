
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

package com.bytechef.helios.configuration.filesystem;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.configuration.constant.ProjectConstants;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnExpression("""
    '${bytechef.coordinator.enabled:true}' == 'true' and '${bytechef.workflow.repository.filesystem.enabled}' == 'true'
    """)
public class ProjectOrphanFilesystemWorkflowChecker {

    private static final Logger logger = LoggerFactory.getLogger(ProjectOrphanFilesystemWorkflowChecker.class);

    private final String basePath;
    private final ProjectService projectService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectOrphanFilesystemWorkflowChecker(
        @Value("${bytechef.workflow.repository.filesystem.projects.base-path:}") String basePath,
        ProjectService projectService, TagService tagService, WorkflowService workflowService) {

        this.basePath = basePath;
        this.projectService = projectService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    @SuppressFBWarnings("NP")
    public void onApplicationReadyEvent() {
        List<Project> projects = projectService.getProjects();
        List<Workflow> workflows = workflowService.getFilesystemWorkflows(ProjectConstants.PROJECT_TYPE);

        List<Workflow> orphanWorkflows = new ArrayList<>();
        List<String> projectWorkflowIds = CollectionUtils.flatMap(projects, Project::getWorkflowIds);

        for (Workflow workflow : workflows) {
            String workflowId = workflow.getId();

            if (!CollectionUtils.contains(projectWorkflowIds, workflowId)) {
                orphanWorkflows.add(workflow);
            }
        }

        if (!orphanWorkflows.isEmpty()) {
            for (Workflow workflow : orphanWorkflows) {
                String path = (String) workflow.getMetadata(WorkflowConstants.PATH);
                String projectName;

                path = path.replace("file:" + basePath + (basePath.endsWith(File.separator) ? "" : File.separator), "");

                String[] items = Objects.requireNonNull(StringUtils.tokenizeToStringArray(path, File.separator));

                if (items.length == 2) {
                    projectName = getProjectName(items[0]);
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(
                            "Workflow id={} is in the wrong location, cannot be assigned to any project",
                            workflow.getId());
                    }

                    continue;
                }

                Project project = projectService.fetchProject(projectName)
                    .orElseGet(() -> projectService.create(
                        Project.builder()
                            .name(projectName)
                            .status(Project.Status.PUBLISHED)
                            .tagIds(getTagIds(workflow.getSourceType()))
                            .build()));

                projectService.addWorkflow(Objects.requireNonNull(project.getId()), workflow.getId());
            }
        }
    }

    private String getProjectName(String name) {
        return String.join(" ", name.split("_"));
    }

    private List<Long> getTagIds(Workflow.SourceType sourceType) {
        String sourceTypeName = sourceType.name();

        return CollectionUtils.map(tagService.save(List.of(new Tag(sourceTypeName.toLowerCase()))), Tag::getId);
    }
}
