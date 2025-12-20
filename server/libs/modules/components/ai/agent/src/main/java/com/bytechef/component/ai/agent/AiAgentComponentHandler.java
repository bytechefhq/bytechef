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

package com.bytechef.component.ai.agent;

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.AI_AGENT;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.action.AiAgentChatAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.AiAgentComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(AI_AGENT + "_v1_ComponentHandler")
public class AiAgentComponentHandler implements ComponentHandler {

    private final AiAgentComponentDefinition componentDefinition;

    public AiAgentComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new AiAgentComponentDefinitionImpl(
            component(AI_AGENT)
                .title("AI Agent")
                .description("With the AI Agent, you can chat with the AI agent.")
                .icon("path:assets/ai-agent.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(new AiAgentChatAction(clusterElementDefinitionService).actionDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AiAgentComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements AiAgentComponentDefinition {

        public AiAgentComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
