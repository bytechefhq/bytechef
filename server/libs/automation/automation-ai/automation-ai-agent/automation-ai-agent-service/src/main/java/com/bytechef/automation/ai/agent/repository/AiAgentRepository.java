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

import com.bytechef.automation.ai.agent.domain.AiAgent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link AiAgent} entities.
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiAgentRepository extends ListCrudRepository<AiAgent, Long> {

    /**
     * Finds the agent that belongs to the specified project.
     *
     * @param projectId the ID of the project to filter by
     * @return the agent with the specified project ID, if any
     */
    Optional<AiAgent> findByProjectId(long projectId);

    /**
     * Finds all agents that belong to the specified workspace.
     *
     * @param workspaceId the ID of the workspace to filter by
     * @return a list of agents with the specified workspace ID
     */
    List<AiAgent> findByWorkspaceId(Long workspaceId);

    /**
     * Finds all agents that reference the given agent as a sub-agent, i.e. agents that have an {@code ai_agent_element}
     * row with {@code kind = SUB_AGENT} and {@code reference_id = referencedAgentId}.
     *
     * @param referencedAgentId the ID of the agent being referenced as a sub-agent
     * @return the referencing agents
     */
    @Query("""
        SELECT ai_agent.* FROM ai_agent
        JOIN ai_agent_element ON ai_agent_element.agent_id = ai_agent.id
        WHERE ai_agent_element.kind = 'SUB_AGENT' AND ai_agent_element.reference_id = :referencedAgentId
        ORDER BY ai_agent.id ASC
        """)
    List<AiAgent> findSubAgentReferencingAgents(@Param("referencedAgentId") long referencedAgentId);
}
