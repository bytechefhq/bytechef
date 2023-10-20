
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
import com.bytechef.atlas.domain.Workflow.SourceType;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DemoProjectConfiguration implements InitializingBean {

    private final ProjectService projectService;
    private final ResourcePatternResolver resourcePatternResolver;
    private final TagService tagService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public DemoProjectConfiguration(
        ProjectService projectService, ResourcePatternResolver resourcePatternResolver, TagService tagService,
        WorkflowService workflowService) {

        this.projectService = projectService;
        this.resourcePatternResolver = resourcePatternResolver;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional
    @SuppressFBWarnings("NP")
    public void afterPropertiesSet() throws Exception {
        if (projectService.countProjects() == 0) {
            Project project = projectService.create(
                Project.builder()
                    .name("Demo")
                    .status(Project.Status.PUBLISHED)
                    .tagIds(
                        tagService.save(List.of(new Tag("demo")))
                            .stream()
                            .map(Tag::getId)
                            .toList())
                    .build());

            for (Resource resource : resourcePatternResolver.getResources("classpath:demo/*.yaml")) {
                Workflow workflow = workflowService.create(
                    resource.getContentAsString(StandardCharsets.UTF_8), Workflow.Format.YAML, SourceType.JDBC);

                projectService.addWorkflow(Objects.requireNonNull(project.getId()), workflow.getId());
            }
        }
    }
}
