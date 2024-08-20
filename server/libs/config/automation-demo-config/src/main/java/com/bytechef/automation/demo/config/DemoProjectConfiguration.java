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

package com.bytechef.automation.demo.config;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCoordinator
@DependsOn("mapUtils")
public class DemoProjectConfiguration {

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ResourcePatternResolver resourcePatternResolver;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DemoProjectConfiguration(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        ResourcePatternResolver resourcePatternResolver, TagService tagService, WorkflowService workflowService) {

        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.resourcePatternResolver = resourcePatternResolver;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Bean
    @Transactional
    ApplicationRunner demoProjectApplicationRunner() {
        return args -> {
            if (projectService.countProjects() == 0) {
                Project project = projectService.create(
                    Project.builder()
                        .name("Demo")
                        .tagIds(
                            tagService.save(List.of(new Tag("demo")))
                                .stream()
                                .map(Tag::getId)
                                .toList())
                        .workspaceId(1049)
                        .build());

                for (Resource resource : resourcePatternResolver.getResources("classpath:demo/*.json")) {
                    Workflow workflow = workflowService.create(
                        resource.getContentAsString(StandardCharsets.UTF_8), Workflow.Format.JSON, SourceType.JDBC);

                    projectWorkflowService.addWorkflow(
                        Validate.notNull(project.getId(), "id"), project.getLastProjectVersion(), workflow.getId());
                }
            }
        };
    }
}
