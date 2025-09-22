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

package com.bytechef.automation.configuration.web.rest.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 *
 */
@ComponentScan(basePackages = {
    "com.bytechef.automation.configuration.web.rest",
    "com.bytechef.automation.configuration.web.rest.adapter",
    "com.bytechef.automation.configuration.web.rest.mapper",
    "com.bytechef.platform.configuration.web.rest.adapter",
    "com.bytechef.platform.configuration.web.rest.mapper",
    "com.bytechef.web.rest.mapper"
})
@Configuration
@Import(JacksonConfiguration.class)
public class AutomationConfigurationRestTestConfiguration {

    @Bean
    ComponentConnectionFacade componentConnectionFacade() {
        return mock(ComponentConnectionFacade.class);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService() {
        return mock(ComponentDefinitionService.class);
    }

    @Bean
    WorkflowService workflowService() {
        return mock(WorkflowService.class);
    }

    @Bean
    WorkflowFacade workflowFacade() {
        return mock(WorkflowFacade.class);
    }
}
