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

package com.bytechef.platform.configuration.web.rest.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.service.UnifiedApiDefinitionService;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeDescriptionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeDynamicPropertiesFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOptionFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeScriptFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.web.rest.mapper.DateTimeMapper;
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
    ActionDefinitionFacade.class, ActionDefinitionService.class, ClusterElementDefinitionService.class,
    ComponentDefinitionService.class, ConnectionDefinitionService.class, DateTimeMapper.class,
    EnvironmentService.class, OAuth2ParametersFacade.class, OAuth2Service.class, TaskDispatcherDefinitionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, UnifiedApiDefinitionService.class,
    WorkflowNodeDescriptionFacade.class, WorkflowNodeDynamicPropertiesFacade.class, WorkflowNodeOptionFacade.class,
    WorkflowNodeOutputFacade.class, WorkflowNodeParameterFacade.class, WebhookTriggerTestFacade.class,
    WorkflowNodeScriptFacade.class, WorkflowNodeTestOutputFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class
})
public @interface WorkflowConfigurationRestTestConfigurationSharedMocks {
}
