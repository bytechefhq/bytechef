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
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsAppendFilesToAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsCreateAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsDeleteAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsRemoveFileFromAiSkillAction;
import com.bytechef.component.ai.agent.utils.action.AiAgentUtilsUpdateAiSkillAction;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsAgentClientTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsAskUserQuestionTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsAutoMemoryTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsBraveWebSearchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsFileSystemTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGlobTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsGrepTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsListDirectoryTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsShellTools;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsSmartWebFetchTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTaskTool;
import com.bytechef.component.ai.agent.utils.cluster.AiAgentUtilsTodoWriteTool;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
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
        AiSkillFacade aiSkillFacade, List<AiAgentUtilsClusterElementContributor> clusterElementContributors,
        ClusterElementDefinitionService clusterElementDefinitionService, AiAutoMemoryService aiAutoMemoryService) {

        AiAgentUtilsSmartWebFetchTool agentUtilsSmartWebFetchTool = new AiAgentUtilsSmartWebFetchTool(
            clusterElementDefinitionService);

        AiAgentUtilsTaskTool agentUtilsTaskTool = new AiAgentUtilsTaskTool(clusterElementDefinitionService);

        AiAgentUtilsAutoMemoryTool agentUtilsAutoMemoryTool = new AiAgentUtilsAutoMemoryTool(aiAutoMemoryService);

        List<ClusterElementDefinition<?>> clusterElements = new ArrayList<>(List.of(
            // Delegates a task to a remote A2A agent. The io.a2a client transport is passed explicitly (no
            // ServiceLoader discovery), so class loading is safe; a transport failure surfaces only at execution time.
            AiAgentUtilsAgentClientTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsAskUserQuestionTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsFileSystemTools.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsShellTools.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsGrepTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsGlobTool.CLUSTER_ELEMENT_DEFINITION,
            AiAgentUtilsListDirectoryTool.CLUSTER_ELEMENT_DEFINITION,
            agentUtilsSmartWebFetchTool.clusterElementDefinition,
            AiAgentUtilsBraveWebSearchTool.CLUSTER_ELEMENT_DEFINITION,
            agentUtilsAutoMemoryTool.clusterElementDefinition,
            AiAgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION,
            agentUtilsTaskTool.clusterElementDefinition,
            tool(AiAgentUtilsAppendFilesToAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsCreateAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsDeleteAiSkillAction.of(aiSkillFacade)),
            tool(AiAgentUtilsRemoveFileFromAiSkillAction.of(aiSkillFacade)),
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
                AiAgentUtilsAppendFilesToAiSkillAction.of(aiSkillFacade),
                AiAgentUtilsCreateAiSkillAction.of(aiSkillFacade),
                AiAgentUtilsDeleteAiSkillAction.of(aiSkillFacade),
                AiAgentUtilsRemoveFileFromAiSkillAction.of(aiSkillFacade),
                AiAgentUtilsUpdateAiSkillAction.of(aiSkillFacade))
            .clusterElements(clusterElements);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
