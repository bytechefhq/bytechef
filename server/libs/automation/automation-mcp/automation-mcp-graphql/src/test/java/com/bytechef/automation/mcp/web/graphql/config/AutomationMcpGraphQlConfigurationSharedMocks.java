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

package com.bytechef.automation.mcp.web.graphql.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.mcp.facade.McpProjectFacade;
import com.bytechef.automation.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
    ApplicationProperties.class, McpProjectFacade.class, McpProjectService.class,
    McpProjectWorkflowService.class, ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class,
    ProjectService.class, WorkflowService.class, WorkspaceMcpServerFacade.class
})
public @interface AutomationMcpGraphQlConfigurationSharedMocks {
}
