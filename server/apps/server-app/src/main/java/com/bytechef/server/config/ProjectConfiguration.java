
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

package com.bytechef.server.config;

import com.bytechef.atlas.job.JobFactory;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.category.service.CategoryService;
import com.bytechef.helios.project.facade.ProjectFacade;
import com.bytechef.helios.project.facade.ProjectFacadeImpl;
import com.bytechef.helios.project.facade.ProjectInstanceFacade;
import com.bytechef.helios.project.facade.ProjectInstanceFacadeImpl;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.helios.project.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.tag.service.TagService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ProjectConfiguration {

    @Bean
    ProjectInstanceFacade projectInstanceFacade(
        JobFactory jobFactory, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService) {

        return new ProjectInstanceFacadeImpl(
            jobFactory, projectInstanceService, projectInstanceWorkflowService, projectService, tagService);
    }

    @Bean
    ProjectFacade projectFacade(
        CategoryService categoryService, JobService jobService, ProjectInstanceService projectInstanceService,
        ProjectService projectService, TaskExecutionService taskExecutionService, TagService tagService,
        WorkflowService workflowService) {

        return new ProjectFacadeImpl(
            categoryService, jobService, projectInstanceService, projectService, taskExecutionService, tagService,
            workflowService);
    }
}
