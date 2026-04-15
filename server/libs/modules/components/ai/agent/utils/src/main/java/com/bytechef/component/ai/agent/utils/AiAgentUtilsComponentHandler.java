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

import com.bytechef.ai.agent.skill.facade.AiAgentSkillFacade;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsAskUserQuestionTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsBraveWebSearchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsFileSystemTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGlobTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGrepTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsShellTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSkillsTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSmartWebFetchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTaskTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTodoWriteTool;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("aiAgentUtils_v1_ComponentHandler")
public class AiAgentUtilsComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public AiAgentUtilsComponentHandler(AiAgentSkillFacade aiAgentSkillFacade) {
        AiAgentUtilsSkillsTool agentUtilsSkillsTool = new AiAgentUtilsSkillsTool(aiAgentSkillFacade);

        this.componentDefinition = component("aiAgentUtils")
            .title("AI Agent Utils")
            .description("AI Agent Utils brings Claude Code-inspired tools and agent skills.")
            .icon("path:assets/agent-utils.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .clusterElements(
                AiAgentUtilsAskUserQuestionTool.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsFileSystemTools.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsShellTools.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsGrepTool.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsGlobTool.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsSmartWebFetchTool.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsBraveWebSearchTool.CLUSTER_ELEMENT_DEFINITION,
                agentUtilsSkillsTool.clusterElementDefinition,
                AiAgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION,
                AiAgentUtilsTaskTool.CLUSTER_ELEMENT_DEFINITION);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
