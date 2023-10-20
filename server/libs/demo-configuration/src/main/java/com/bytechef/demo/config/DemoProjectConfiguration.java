
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

import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DemoProjectConfiguration implements InitializingBean {

    private static final String DEFAULT = "Default";

    private final ProjectService projectService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DemoProjectConfiguration(ProjectService projectService, WorkflowService workflowService) {
        this.projectService = projectService;
        this.workflowService = workflowService;
    }

    @Override
    public void afterPropertiesSet() {
        List<Project> projects = projectService.getProjects();
        List<Workflow> workflows = workflowService.getWorkflows();

        List<String> projectWorkflowIds = CollectionUtils.map(projects, Project::getWorkflowIds)
            .stream()
            .flatMap(Collection::stream)
            .toList();

        List<String> orphanWorkflowIds = new ArrayList<>();

        for (Workflow workflow : workflows) {
            String workflowId = workflow.getId();

            if (!CollectionUtils.contains(projectWorkflowIds, workflowId)) {
                orphanWorkflowIds.add(workflowId);
            }
        }

        if (!orphanWorkflowIds.isEmpty()) {
            projectService.fetchProject(DEFAULT)
                .ifPresentOrElse(project -> {
                    for (String workflowId : orphanWorkflowIds) {
                        projectService.addWorkflow(project.getId(), workflowId);
                    }
                },
                    () -> projectService.create(
                        Project.builder()
                            .name(DEFAULT)
                            .status(Project.Status.PUBLISHED)
                            .workflowIds(orphanWorkflowIds)
                            .build()));
        }
    }
}
