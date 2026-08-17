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

package com.bytechef.automation.ai.agent.repository;

import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link AiAgentElement} entities.
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiAgentElementRepository extends ListCrudRepository<AiAgentElement, Long> {

    /**
     * Finds all elements that belong to the specified agent, ordered by {@code position} ascending.
     *
     * @param agentId the ID of the agent to filter by
     * @return the position-ordered elements
     */
    @Query("""
        SELECT * FROM ai_agent_element
        WHERE agent_id = :agentId
        ORDER BY position ASC
        """)
    List<AiAgentElement> findAllByAgentIdOrderByPositionAsc(@Param("agentId") long agentId);
}
