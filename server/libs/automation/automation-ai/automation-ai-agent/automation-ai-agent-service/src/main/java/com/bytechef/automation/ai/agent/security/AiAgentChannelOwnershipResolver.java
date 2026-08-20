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

package com.bytechef.automation.ai.agent.security;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an agent-channel id to its owning workspace by traversing channel &rarr; agent &rarr;
 * {@code ai_agent.workspace_id}. Reads the repositories directly (not the {@code @PreAuthorize}-guarded facade) to
 * avoid recursion. Fails closed when either hop cannot be resolved.
 *
 * <p>
 * A channel row exists so {@code updateAgentChannel} / {@code deleteAgentChannel} can be gated at all: they are keyed
 * on the channel's own id, and nothing in the argument list names the agent or the workspace. Without this hop a
 * channel id belonging to another workspace's agent would be indistinguishable from one's own.
 *
 * <p>
 * {@code AiAgentChannel.agentId} is a primitive {@code long}, so the first hop cannot be null; the second can —
 * {@link AiAgent#getWorkspaceId()} is {@code @Nullable Long} and the tail here matches {@link AiAgentOwnershipResolver}
 * exactly so the child path and the agent path cannot disagree about a workspace-less agent.
 *
 * @author Ivica Cardic
 */
@Component
public class AiAgentChannelOwnershipResolver implements ResourceOwnershipResolver {

    private final AiAgentChannelRepository aiAgentChannelRepository;
    private final AiAgentRepository aiAgentRepository;

    @SuppressFBWarnings("EI")
    public AiAgentChannelOwnershipResolver(
        AiAgentChannelRepository aiAgentChannelRepository, AiAgentRepository aiAgentRepository) {

        this.aiAgentChannelRepository = aiAgentChannelRepository;
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    public String resourceType() {
        return "AiAgentChannel";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return aiAgentChannelRepository.findById(id)
            .map(AiAgentChannel::getAgentId)
            .flatMap(aiAgentRepository::findById)
            .map(AiAgent::getWorkspaceId)
            .map(ResourceOwner::ofWorkspace)
            .orElseGet(ResourceOwner::unknown);
    }
}
