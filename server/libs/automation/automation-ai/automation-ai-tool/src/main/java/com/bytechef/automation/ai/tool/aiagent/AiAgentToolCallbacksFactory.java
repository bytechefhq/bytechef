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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the AI Agent (agent-builder) tool-callback lists shared by the Copilot agents and the AI Hub
 * {@code ai_agent_agent} subagent. Read list feeds ASK; write list feeds BUILD.
 *
 * @author Ivica Cardic
 */
public class AiAgentToolCallbacksFactory {

    private final AiAgentFacade aiAgentFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAgentToolCallbacksFactory(AiAgentFacade aiAgentFacade) {
        this.aiAgentFacade = aiAgentFacade;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListAiAgentsToolCallback(aiAgentFacade));
        toolCallbacks.add(new GetAiAgentToolCallback(aiAgentFacade));

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
        toolCallbacks.add(new PublishAiAgentToolCallback(aiAgentFacade));

        return toolCallbacks;
    }
}
