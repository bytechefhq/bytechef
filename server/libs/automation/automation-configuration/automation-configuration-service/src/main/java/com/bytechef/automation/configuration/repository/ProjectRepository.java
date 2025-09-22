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

package com.bytechef.automation.configuration.repository;

import com.bytechef.automation.configuration.domain.Project;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectRepository
    extends ListPagingAndSortingRepository<Project, Long>, CrudRepository<Project, Long>, CustomProjectRepository {

    @Query("""
            SELECT project.* FROM project
            JOIN project_deployment ON project.id = project_deployment.project_id
            WHERE project_deployment.id = :projectDeploymentId
        """)
    Optional<Project> findByProjectDeploymentId(@Param("projectDeploymentId") long projectDeploymentId);

    Optional<Project> findByNameIgnoreCase(String name);

    Optional<Project> findByUuid(UUID uuid);

    @Query("""
            SELECT project.* FROM project
            JOIN project_workflow ON project.id = project_workflow.project_id
            WHERE project_workflow.workflow_id = :workflowId
        """)
    Optional<Project> findByWorkflowId(@Param("workflowId") String workflowId);
}
