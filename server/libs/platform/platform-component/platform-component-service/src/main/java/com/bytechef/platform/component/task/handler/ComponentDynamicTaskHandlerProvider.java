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

package com.bytechef.platform.component.task.handler;

import com.bytechef.atlas.worker.task.handler.DynamicTaskHandlerProvider;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.definition.WorkflowNodeType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order
public class ComponentDynamicTaskHandlerProvider implements DynamicTaskHandlerProvider {

    private final ActionDefinitionFacade actionDefinitionFacade;

    public ComponentDynamicTaskHandlerProvider(ActionDefinitionFacade actionDefinitionFacade) {
        this.actionDefinitionFacade = actionDefinitionFacade;
    }

    @Override
    public TaskHandler<?> getTaskHandler(String type) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        return new ComponentTaskHandler(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), actionDefinitionFacade);
    }
}
