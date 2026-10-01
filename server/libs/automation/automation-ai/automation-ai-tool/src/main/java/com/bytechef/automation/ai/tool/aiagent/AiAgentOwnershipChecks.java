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

import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
final class AiAgentOwnershipChecks {

    private AiAgentOwnershipChecks() {
    }

    static @Nullable String elementOwnershipError(AiAgentFacade aiAgentFacade, long agentId, long elementId) {
        AiAgentDTO agentDTO = aiAgentFacade.getAgent(agentId);

        for (AiAgentElement element : agentDTO.elements()) {
            if (Objects.equals(element.getId(), elementId)) {
                return null;
            }
        }

        return "Element " + elementId + " does not belong to agent " + agentId +
            "; call getAiAgent and use one of its elements' ids";
    }

    static @Nullable String channelOwnershipError(AiAgentFacade aiAgentFacade, long agentId, long channelId) {
        AiAgentDTO agentDTO = aiAgentFacade.getAgent(agentId);

        for (AiAgentChannel channel : agentDTO.channels()) {
            if (Objects.equals(channel.getId(), channelId)) {
                return null;
            }
        }

        return "Channel " + channelId + " does not belong to agent " + agentId +
            "; call getAiAgent and use one of its channels' ids";
    }
}
