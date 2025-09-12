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

package com.bytechef.automation.configuration.util;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ComponentDefinitionHelper {

    private final ComponentDefinitionService componentDefinitionService;

    public ComponentDefinitionHelper(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    public List<ComponentDefinition> getComponentDefinitions(Workflow workflow) {
        Set<ComponentDefinition> componentInfos = new HashSet<>();

        for (WorkflowTask workflowTask : workflow.getTasks(true)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (workflowNodeType.operation() == null) {
                continue;
            }

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            componentInfos.add(componentDefinition);
        }

        return new ArrayList<>(componentInfos);
    }
}
