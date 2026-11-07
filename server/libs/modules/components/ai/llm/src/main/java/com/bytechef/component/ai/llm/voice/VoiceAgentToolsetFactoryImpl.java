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

package com.bytechef.component.ai.llm.voice;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.tool.ClusterElementToolCallbacks;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolsetFactory;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public final class VoiceAgentToolsetFactoryImpl implements VoiceAgentToolsetFactory {

    private final ClusterElementToolCallbacks clusterElementToolCallbacks;

    public VoiceAgentToolsetFactoryImpl(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService) {

        this.clusterElementToolCallbacks = new ClusterElementToolCallbacks(
            aiAgentToolFacade, clusterElementDefinitionService);
    }

    @Override
    public VoiceAgentToolset create(
        Map<String, ?> extensions, Map<String, ComponentConnection> connections, ActionContext context) {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement toolClusterElement : clusterElementMap.getClusterElements(BaseToolFunction.TOOLS)) {
            toolCallbacks.addAll(
                clusterElementToolCallbacks.build(toolClusterElement, connections, context));
        }

        return toolCallbacks.isEmpty() ? VoiceAgentToolset.EMPTY : new ToolCallbackVoiceAgentToolset(toolCallbacks);
    }
}
