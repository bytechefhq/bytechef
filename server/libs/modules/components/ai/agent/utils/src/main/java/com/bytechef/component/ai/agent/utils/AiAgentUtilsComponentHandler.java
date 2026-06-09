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
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsCreateAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsDeleteAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsUpdateAiSkillAction;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsBraveWebSearchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsFileSystemTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGlobTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGrepTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsShellTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSmartWebFetchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTaskTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTodoWriteTool;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component("aiAgentUtils_v1_ComponentHandler")
public class AiAgentUtilsComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public AiAgentUtilsComponentHandler(
        AiSkillFacade aiSkillFacade,
        List<AiAgentUtilsClusterElementContributor> clusterElementContributors) {

        List<ClusterElementDefinition<?>> clusterElements = new ArrayList<>(List.of(
            AiAgentUtilsFileSystemTools.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsShellTools.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsGrepTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsGlobTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsSmartWebFetchTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsBraveWebSearchTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsTaskTool.CLUSTER_ELEMENT_DEFINITION,
            tool(AiAgentUtilsCreateAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsDeleteAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsUpdateAiSkillAction.of(aiSkillFacade))));

        for (AiAgentUtilsClusterElementContributor clusterElementContributor : clusterElementContributors) {
            clusterElements.add(clusterElementContributor.getClusterElementDefinition());
        }

        this.componentDefinition = component("aiAgentUtils")
            .title("AI Agent Utils")
            .description("AI Agent Utils brings Claude Code-inspired tools and agent skills.")
            .icon("path:assets/agent-utils.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .actions(
                AiAgentUtilsCreateAiSkillAction.of(aiSkillFacade),
                AiAgentUtilsDeleteAiSkillAction.of(aiSkillFacade),
                AiAgentUtilsUpdateAiSkillAction.of(aiSkillFacade))
            .clusterElements(clusterElements);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
