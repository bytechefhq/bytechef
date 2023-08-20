
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

package com.bytechef.webhook.config;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.bytechef.hermes.definition.registry.component.ComponentOperation;
import com.bytechef.hermes.definition.registry.component.util.ComponentUtils;
import com.bytechef.hermes.definition.registry.service.ActionDefinitionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkerHandlerConfiguration {

    private final ActionDefinitionService actionDefinitionService;

    public WorkerHandlerConfiguration(ActionDefinitionService actionDefinitionService) {
        this.actionDefinitionService = actionDefinitionService;
    }

    @Bean
    TaskHandlerRegistry taskHandlerRegistry() {
        return type -> (TaskHandler<?>) taskExecution -> {
            ComponentOperation componentOperation = ComponentUtils.getComponentOperation(type);

            return actionDefinitionService.executePerform(
                componentOperation.componentName(), componentOperation.componentVersion(),
                componentOperation.operationName(), taskExecution.getId(), taskExecution.getParameters(),
                MapUtils.getMap(taskExecution.getMetadata(), MetadataConstants.CONNECTION_IDS, Long.class));
        };
    }
}
