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

package com.bytechef.automation.ai.a2a.repository;

import com.bytechef.automation.ai.a2a.domain.A2aProject;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link A2aProject} entities.
 *
 * @author Ivica Cardic
 */
@Repository
public interface A2aProjectRepository extends ListCrudRepository<A2aProject, Long> {

    /**
     * Finds all A2A projects that belong to the specified A2A server.
     *
     * @param a2aServerId the ID of the A2A server to filter by
     * @return a list of A2A projects with the specified A2A server ID
     */
    @Query("""
        SELECT * FROM a2a_project
        WHERE a2a_server_id = :a2aServerId
        ORDER BY id ASC
        """)
    List<A2aProject> findAllByA2aServerId(@Param("a2aServerId") Long a2aServerId);

    /**
     * Finds all A2A projects backed by the specified project deployment.
     *
     * @param projectDeploymentId the ID of the project deployment to filter by
     * @return a list of A2A projects with the specified project deployment ID
     */
    @Query("""
        SELECT * FROM a2a_project
        WHERE project_deployment_id = :projectDeploymentId
        ORDER BY id ASC
        """)
    List<A2aProject> findAllByProjectDeploymentId(@Param("projectDeploymentId") Long projectDeploymentId);
}
