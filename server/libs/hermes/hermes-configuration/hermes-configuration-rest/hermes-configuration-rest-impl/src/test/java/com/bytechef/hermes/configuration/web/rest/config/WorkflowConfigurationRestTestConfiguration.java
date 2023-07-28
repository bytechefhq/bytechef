
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

package com.bytechef.hermes.configuration.web.rest.config;

import com.bytechef.hermes.configuration.facade.OAuth2ParameterFacade;
import com.bytechef.hermes.definition.registry.facade.ActionDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.ComponentDefinitionFacade;
import com.bytechef.hermes.definition.registry.facade.TriggerDefinitionFacade;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import com.bytechef.hermes.definition.registry.service.ComponentDefinitionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.definition.registry.service.TaskDispatcherDefinitionService;
import com.bytechef.hermes.definition.registry.service.TriggerDefinitionService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.configuration.service.WorkflowService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.hermes.configuration.web.rest", "com.bytechef.hermes.definition.registry"
})
@SpringBootConfiguration
public class WorkflowConfigurationRestTestConfiguration {

    @MockBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @MockBean
    private ActionDefinitionService actionDefinitionService;

    @MockBean
    private ComponentDefinitionFacade componentDefinitionFacade;

    @MockBean
    private ComponentDefinitionService componentDefinitionService;

    @MockBean
    private ConnectionDefinitionService connectionDefinitionService;

    @MockBean
    private MessageBroker messageBroker;

    @MockBean
    private OAuth2ParameterFacade oAuth2ParameterFacade;

    @MockBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @MockBean
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @MockBean
    private TriggerDefinitionService triggerDefinitionService;

    @MockBean
    private WorkflowService workflowService;
}
