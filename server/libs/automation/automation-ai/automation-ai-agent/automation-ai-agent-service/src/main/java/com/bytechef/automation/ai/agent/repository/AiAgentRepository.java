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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface AiAgentRepository extends ListCrudRepository<AiAgent, Long> {

    @Query("""
        SELECT ai_agent.* FROM ai_agent
        WHERE ai_agent.project_workflow_uuid IN (
            SELECT project_workflow.uuid FROM project_workflow WHERE project_workflow.project_id = :projectId
        )
        ORDER BY ai_agent.id ASC
        """)
    List<AiAgent> findAllByProjectId(@Param("projectId") long projectId);

    Optional<AiAgent> findByUuid(UUID uuid);

    @Query("SELECT DISTINCT project_workflow.project_id FROM project_workflow WHERE project_workflow.uuid = :uuid")
    Optional<Long> findProjectIdByProjectWorkflowUuid(@Param("uuid") UUID projectWorkflowUuid);

    @Query(
        value = """
            SELECT DISTINCT project_workflow.uuid, project_workflow.project_id FROM project_workflow
            WHERE project_workflow.uuid IN (:uuids)
            """,
        rowMapperClass = ProjectWorkflowProjectRowMapper.class)
    List<ProjectWorkflowProject> findProjectIdsByProjectWorkflowUuids(
        @Param("uuids") Collection<UUID> projectWorkflowUuids);

    List<AiAgent> findByWorkspaceId(Long workspaceId);

    @Query("""
        SELECT ai_agent.* FROM ai_agent
        JOIN ai_agent_element ON ai_agent_element.agent_id = ai_agent.id
        WHERE ai_agent_element.kind = 'SUB_AGENT' AND ai_agent_element.reference_id = :referencedAgentId
        ORDER BY ai_agent.id ASC
        """)
    List<AiAgent> findSubAgentReferencingAgents(@Param("referencedAgentId") long referencedAgentId);

    record ProjectWorkflowProject(UUID projectWorkflowUuid, long projectId) {
    }

    class ProjectWorkflowProjectRowMapper implements RowMapper<ProjectWorkflowProject> {

        @Override
        public ProjectWorkflowProject mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            return new ProjectWorkflowProject(
                resultSet.getObject("uuid", UUID.class), resultSet.getLong("project_id"));
        }
    }
}
