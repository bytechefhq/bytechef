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

package com.bytechef.hermes.configuration.web.rest.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.component.registry.service.ActionDefinitionService;
import com.bytechef.hermes.component.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.component.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.configuration.facade.OAuth2ParameterFacade;
import com.bytechef.hermes.oauth2.service.OAuth2Service;
import com.bytechef.hermes.task.dispatcher.registry.service.TaskDispatcherDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.hermes.configuration.web.rest"
})
@Configuration
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class WorkflowConfigurationRestTestConfiguration {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public WorkflowConfigurationRestTestConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @MockBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @MockBean
    private ActionDefinitionService actionDefinitionService;

    @MockBean
    private ComponentDefinitionService componentDefinitionService;

    @MockBean
    private ConnectionDefinitionService connectionDefinitionService;

    @MockBean
    private OAuth2ParameterFacade oAuth2ParameterFacade;

    @MockBean
    private OAuth2Service oAuth2Service;

    @MockBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @MockBean
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @MockBean
    private TriggerDefinitionService triggerDefinitionService;

    @MockBean
    private WorkflowService workflowService;

    @Bean
    JsonUtils jsonUtils() {
        return new JsonUtils() {
            {
                objectMapper = WorkflowConfigurationRestTestConfiguration.this.objectMapper;
            }
        };
    }

    @Bean
    MapUtils mapUtils() {
        return new MapUtils() {
            {
                objectMapper = WorkflowConfigurationRestTestConfiguration.this.objectMapper;
            }
        };
    }
}
