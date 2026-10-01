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

package com.bytechef.automation.ai.tool.aiagent;

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class AiAgentToolCallbacksFactory {

    private final AiAgentFacade aiAgentFacade;
    private final @Nullable AiSkillFacade aiSkillFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final @Nullable WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAgentToolCallbacksFactory(
        AiAgentFacade aiAgentFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        @Nullable AiSkillFacade aiSkillFacade, @Nullable WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade) {

        this.aiAgentFacade = aiAgentFacade;
        this.aiSkillFacade = aiSkillFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.workspaceKnowledgeBaseFacade = workspaceKnowledgeBaseFacade;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListAiAgentsToolCallback(aiAgentFacade));
        toolCallbacks.add(new GetAiAgentToolCallback(aiAgentFacade));
        toolCallbacks.add(new SearchAiAgentToolsToolCallback(clusterElementDefinitionService));
        toolCallbacks.add(new SearchAiAgentReferencesToolCallback(aiSkillFacade, workspaceKnowledgeBaseFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateAiAgentToolCallback(aiAgentFacade));
        toolCallbacks.add(new UpdateAiAgentToolCallback(aiAgentFacade));
        toolCallbacks.add(new AddAiAgentChannelToolCallback(aiAgentFacade));
        toolCallbacks.add(new DeleteAiAgentChannelToolCallback(aiAgentFacade));
        toolCallbacks.add(new AddAiAgentElementToolCallback(aiAgentFacade));
        toolCallbacks.add(new UpdateAiAgentElementToolCallback(aiAgentFacade));
        toolCallbacks.add(new DeleteAiAgentElementToolCallback(aiAgentFacade));
        toolCallbacks.add(new UpdateAiAgentSettingsToolCallback(aiAgentFacade));

        return toolCallbacks;
    }
}
