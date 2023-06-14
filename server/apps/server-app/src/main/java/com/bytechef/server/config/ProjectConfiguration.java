
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

import com.bytechef.atlas.execution.job.JobFactory;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.category.service.CategoryService;
import com.bytechef.helios.configuration.facade.ProjectFacade;
import com.bytechef.helios.configuration.facade.ProjectFacadeImpl;
import com.bytechef.helios.configuration.facade.ProjectInstanceFacade;
import com.bytechef.helios.configuration.facade.ProjectInstanceFacadeImpl;
import com.bytechef.helios.execution.job.ProjectInstanceWorkflowJobFactory;
import com.bytechef.helios.execution.job.ProjectInstanceWorkflowJobFactoryImpl;
import com.bytechef.helios.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.helios.execution.facade.ProjectWorkflowExecutionFacadeImpl;
import com.bytechef.helios.configuration.service.ProjectInstanceService;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.helios.configuration.service.ProjectService;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.execution.trigger.lifecycle.TriggerLifecycleManager;
import com.bytechef.hermes.execution.facade.JobFacade;
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
        ConnectionService connectionService, ProjectInstanceService projectInstanceService,
        ProjectInstanceWorkflowService projectInstanceWorkflowService, ProjectService projectService,
        TagService tagService, TriggerLifecycleManager triggerLifecycleManager, WorkflowService workflowService) {

        return new ProjectInstanceFacadeImpl(
            connectionService, projectInstanceService, projectInstanceWorkflowService, projectService, tagService,
            triggerLifecycleManager, workflowService);
    }

    @Bean
    ProjectInstanceWorkflowJobFactory projectInstanceWorkflowJobFactory(
        JobFactory jobFactory, ProjectInstanceWorkflowService projectInstanceWorkflowService) {

        return new ProjectInstanceWorkflowJobFactoryImpl(jobFactory, projectInstanceWorkflowService);
    }

    @Bean
    ProjectFacade projectFacade(
        CategoryService categoryService, ProjectInstanceService projectInstanceService, ProjectService projectService,
        TagService tagService, WorkflowService workflowService) {

        return new ProjectFacadeImpl(
            categoryService, projectInstanceService, projectService, tagService, workflowService);
    }

    @Bean
    ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade(
        JobFacade jobFacade, JobService jobService, ProjectInstanceService projectInstanceService,
        ProjectService projectService, WorkflowService workflowService) {

        return new ProjectWorkflowExecutionFacadeImpl(
            jobFacade, jobService, projectInstanceService, projectService, workflowService);
    }
}
