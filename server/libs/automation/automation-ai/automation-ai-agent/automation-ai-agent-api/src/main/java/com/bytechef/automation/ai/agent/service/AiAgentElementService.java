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

import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import java.util.List;

/**
 * Service interface for managing {@link AiAgentElement} entities.
 *
 * @author Ivica Cardic
 */
public interface AiAgentElementService {

    AiAgentElement create(AiAgentElement agentElement);

    void delete(long id);

    /**
     * Returns the element with the given id.
     *
     * @param id the element id
     * @return the element
     * @throws IllegalArgumentException if no element with that id exists
     */
    AiAgentElement getAgentElement(long id);

    /**
     * Returns the elements of the given agent, ordered by {@code position} ascending.
     *
     * @param agentId the agent id
     * @return the position-ordered elements
     */
    List<AiAgentElement> getByAgentId(long agentId);

    AiAgentElement update(AiAgentElement agentElement);
}
