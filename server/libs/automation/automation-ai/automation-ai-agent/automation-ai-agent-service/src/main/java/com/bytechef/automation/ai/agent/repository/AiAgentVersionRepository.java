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

import com.bytechef.automation.ai.agent.domain.AiAgentVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface AiAgentVersionRepository extends ListCrudRepository<AiAgentVersion, Long> {

    @Modifying
    @Query("DELETE FROM ai_agent_version WHERE project_id = :projectId")
    void deleteByProjectId(@Param("projectId") long projectId);

    @Query("""
        SELECT * FROM ai_agent_version
        WHERE project_id = :projectId AND project_version = :projectVersion
        ORDER BY id ASC
        """)
    List<AiAgentVersion> findAllByProjectIdAndProjectVersion(
        @Param("projectId") long projectId, @Param("projectVersion") int projectVersion);

    @Query("""
        SELECT * FROM ai_agent_version
        WHERE project_workflow_uuid = :projectWorkflowUuid AND project_version = :projectVersion
        """)
    Optional<AiAgentVersion> findByProjectWorkflowUuidAndProjectVersion(
        @Param("projectWorkflowUuid") UUID projectWorkflowUuid, @Param("projectVersion") int projectVersion);
}
