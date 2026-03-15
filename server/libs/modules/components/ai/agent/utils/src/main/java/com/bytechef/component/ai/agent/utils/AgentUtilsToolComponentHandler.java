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

package com.bytechef.component.ai.agent.utils;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.ai.agent.skill.facade.AgentSkillFacade;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("aiAgentUtils_v1_ComponentHandler")
public class AgentUtilsToolComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public AgentUtilsToolComponentHandler(AgentSkillFacade agentSkillFacade) {
        AgentUtilsSkillsTool agentUtilsSkillsTool = new AgentUtilsSkillsTool(agentSkillFacade);

        this.componentDefinition = component("aiAgentUtils")
            .title("AI Agent Utils")
            .description("AI Agent Utils brings Claude Code-inspired tools and agent skills.")
            .icon("path:assets/agent-utils.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .clusterElements(
                AgentUtilsFileSystemTools.CLUSTER_ELEMENT_DEFINITION,
                AgentUtilsShellTools.CLUSTER_ELEMENT_DEFINITION,
                AgentUtilsGrepTool.CLUSTER_ELEMENT_DEFINITION,
                AgentUtilsGlobTool.CLUSTER_ELEMENT_DEFINITION,
                AgentUtilsSmartWebFetchTool.CLUSTER_ELEMENT_DEFINITION,
                AgentUtilsBraveWebSearchTool.CLUSTER_ELEMENT_DEFINITION,
                agentUtilsSkillsTool.clusterElementDefinition,
                AgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION,
                AgentUtilsTaskTool.CLUSTER_ELEMENT_DEFINITION);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
