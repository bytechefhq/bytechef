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

package com.bytechef.automation.configuration.tool.config;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.tool.DeploymentToolCallbacksFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
public class DeploymentToolCallbacksFactoryConfiguration {

    @Bean
    @ConditionalOnBean({
        ProjectDeploymentFacade.class, ProjectWorkflowService.class
    })
    DeploymentToolCallbacksFactory deploymentToolCallbacksFactory(
        ProjectDeploymentFacade projectDeploymentFacade, ProjectWorkflowService projectWorkflowService) {

        return new DeploymentToolCallbacksFactory(projectDeploymentFacade, projectWorkflowService);
    }
}
