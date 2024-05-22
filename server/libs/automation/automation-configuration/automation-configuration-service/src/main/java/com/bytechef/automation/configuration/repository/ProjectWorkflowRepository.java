/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectWorkflowRepository extends ListCrudRepository<ProjectWorkflow, Long> {

    int countByProjectIdAndProjectVersion(long projectId, int projectVersion);

    List<ProjectWorkflow> findAllByProjectId(Long projectId);

    List<ProjectWorkflow> findAllByProjectIdAndProjectVersion(Long projectId, int projectVersion);

    Optional<ProjectWorkflow> findByProjectIdAndProjectVersionAndWorkflowId(
        long projectId, int projectVersion, String workflowId);

    Optional<ProjectWorkflow> findByWorkflowId(String workflowId);

    @Query("""
            SELECT * FROM project_workflow
            JOIN project_instance ON project_instance.project_id = project_workflow.project_id
            AND project_instance.project_version = project_workflow.project_version
            WHERE project_workflow.workflow_reference_code = :workflowReferenceCode
            AND project_instance.id = :projectInstanceId
        """)
    Optional<ProjectWorkflow> findByProjectInstanceIdAndWorkflowReferenceCode(
        @Param("projectInstanceId") long projectInstanceId,
        @Param("workflowReferenceCode") String workflowReferenceCode);
}
