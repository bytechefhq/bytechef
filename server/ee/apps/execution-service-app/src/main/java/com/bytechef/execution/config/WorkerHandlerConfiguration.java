
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

package com.bytechef.execution.config;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.component.registry.service.ActionDefinitionService;
import com.bytechef.hermes.component.registry.service.TriggerDefinitionService;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandler;
import com.bytechef.hermes.worker.trigger.handler.TriggerHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkerHandlerConfiguration {

    private final ActionDefinitionService actionDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;

    public WorkerHandlerConfiguration(
        ActionDefinitionService actionDefinitionService, TriggerDefinitionService triggerDefinitionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Bean
    TaskHandlerRegistry taskHandlerRegistry() {
        return type -> (TaskHandler<?>) taskExecution -> {
            ComponentOperation componentOperation = ComponentOperation.ofType(type);

            return actionDefinitionService.executePerform(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName(), Objects.requireNonNull(taskExecution.getId()),
                taskExecution.getParameters(),
                MapUtils.getMap(taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));
        };
    }

    @Bean
    TriggerHandlerRegistry triggerHandlerRegistry() {
        return type -> (TriggerHandler) triggerExecution -> {
            ComponentOperation componentOperation = ComponentOperation.ofType(type);

            return triggerDefinitionService.executeTrigger(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName(), triggerExecution.getParameters(), triggerExecution.getState(),
                MapUtils.getRequired(
                    triggerExecution.getMetadata(), WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class),
                MapUtils.getMap(triggerExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));
        };
    }
}
