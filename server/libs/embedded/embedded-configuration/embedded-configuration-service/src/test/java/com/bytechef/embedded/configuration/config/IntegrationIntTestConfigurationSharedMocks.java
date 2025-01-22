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

package com.bytechef.embedded.configuration.config;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
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
    ComponentDefinitionService.class, ConnectionDefinitionService.class, ConnectionService.class,
    ConnectedUserService.class, PrincipalJobFacade.class, PrincipalJobService.class, JobFacade.class,
    JobService.class, OAuth2Service.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, ComponentConnectionFacade.class, WorkflowFacade.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationService.class
})
public @interface IntegrationIntTestConfigurationSharedMocks {
}
