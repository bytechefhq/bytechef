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

package com.bytechef.automation.ai.agent.service;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Service interface for managing {@link AiAgent} entities.
 *
 * @author Ivica Cardic
 */
public interface AiAgentService {

    AiAgent create(AiAgent agent);

    void delete(long id);

    Optional<AiAgent> fetchAgent(long id);

    AiAgent getAgent(long id);

    List<AiAgent> getAgents(@Nullable Long workspaceId);

    /**
     * Returns the agents that reference the given agent as a sub-agent, i.e. agents that have an {@code agent_element}
     * row with {@code kind = SUB_AGENT} and {@code reference_id = referencedAgentId}.
     *
     * @param referencedAgentId the id of the agent being referenced as a sub-agent
     * @return the referencing agents
     */
    List<AiAgent> getSubAgentReferencingAgents(long referencedAgentId);

    AiAgent update(AiAgent agent);

    /**
     * Replaces the agent's tag set. Kept separate from {@link #update(AiAgent)} because that method copies a
     * caller-supplied {@link AiAgent} field by field and deliberately leaves the tag collection alone — every
     * draft-affecting mutation path calls it, and none of them carries tags.
     */
    AiAgent update(long id, List<Long> tagIds);
}
