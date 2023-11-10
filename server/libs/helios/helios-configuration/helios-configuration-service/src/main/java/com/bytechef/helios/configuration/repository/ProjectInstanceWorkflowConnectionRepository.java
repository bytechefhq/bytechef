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

package com.bytechef.helios.configuration.repository;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface ProjectInstanceWorkflowConnectionRepository
    extends org.springframework.data.repository.Repository<ProjectInstanceWorkflowConnection, Long> {

    @Query("""
            SELECT project_instance_workflow_connection.* FROM project_instance_workflow_connection
            JOIN project_instance_workflow ON project_instance_workflow_connection.project_instance_workflow_id = project_instance_workflow.id
            WHERE project_instance_workflow.project_instance_id = :projectInstanceId
            AND project_instance_workflow.workflow_id = :workflowId
            AND project_instance_workflow_connection.operation_name = :operationName
            AND project_instance_workflow_connection.key = :key
        """)
    Optional<ProjectInstanceWorkflowConnection> findByProjectInstanceIdAndWorkflowIdAndOperationNameAndKey(
        @Param("projectInstanceId") long projectInstanceId, @Param("workflowId") String workflowId,
        @Param("operationName") String operationName, @Param("key") String key);
}
