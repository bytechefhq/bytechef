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

package com.bytechef.component.ai.agenticai;

import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.AGENTIC_AI;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agenticai.action.AgenticAiRunAction;
import com.bytechef.component.ai.agenticai.cluster.AgenticAiAction;
import com.bytechef.component.ai.agenticai.embabel.EmbabelAgentRunner;
import com.bytechef.component.ai.agenticai.facade.AgenticAiToolFacade;
import com.bytechef.component.ai.agenticai.tool.AgenticAiTool;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.AgenticAiComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.embabel.agent.core.AgentPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(AGENTIC_AI + "_v1_ComponentHandler")
@ConditionalOnBean(AgentPlatform.class)
public class AgenticAiComponentHandler implements ComponentHandler {

    private final AgenticAiComponentDefinition componentDefinition;

    public AgenticAiComponentHandler(
        AgentPlatform agentPlatform, ClusterElementDefinitionService clusterElementDefinitionService,
        AgenticAiToolFacade agenticAiToolFacade) {

        EmbabelAgentRunner embabelAgentRunner = new EmbabelAgentRunner(agentPlatform);

        ActionDefinition agenticAiRunActionDefinition = AgenticAiRunAction.of(
            embabelAgentRunner, clusterElementDefinitionService, agenticAiToolFacade);

        this.componentDefinition = new AgenticAiComponentDefinitionImpl(
            component(AGENTIC_AI)
                .title("Agentic AI")
                .description(
                    "With the Agentic AI, you can define a goal and let the AI autonomously plan and execute " +
                        "tools to achieve it using the Embabel Agent framework with GOAP planning.")
                .icon("path:assets/agentic-ai.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(agenticAiRunActionDefinition)
                .clusterElements(AgenticAiAction.of(), AgenticAiTool.of(agenticAiRunActionDefinition)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AgenticAiComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements AgenticAiComponentDefinition {

        public AgenticAiComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
