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

package com.bytechef.automation.configuration.web.graphql.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.tag.service.TagService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AutomationConfigurationGraphQlTestConfiguration {

    @Bean
    @Primary
    public CategoryService categoryService() {
        return Mockito.mock(CategoryService.class);
    }

    @Bean
    @Primary
    public ProjectFacade projectFacade() {
        return Mockito.mock(ProjectFacade.class);
    }

    @Bean
    @Primary
    public ProjectService projectService() {
        return Mockito.mock(ProjectService.class);
    }

    @Bean
    @Primary
    public ProjectWorkflowFacade projectWorkflowFacade() {
        return Mockito.mock(ProjectWorkflowFacade.class);
    }

    @Bean
    @Primary
    public ProjectWorkflowService projectWorkflowService() {
        return Mockito.mock(ProjectWorkflowService.class);
    }

    @Bean
    @Primary
    public TagService tagService() {
        return Mockito.mock(TagService.class);
    }

    @Bean
    @Primary
    public WorkflowService workflowService() {
        return Mockito.mock(WorkflowService.class);
    }

    @Bean
    @Primary
    public WorkflowFacade workflowFacade() {
        return Mockito.mock(WorkflowFacade.class);
    }

    @Bean
    @Primary
    public WorkspaceConnectionFacade workspaceConnectionFacade() {
        return Mockito.mock(WorkspaceConnectionFacade.class);
    }
}
