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

package com.bytechef.platform.workflow.test.repository;

import com.bytechef.platform.workflow.test.domain.WorkflowTestConfigurationConnection;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface WorkflowTestConfigurationConnectionRepository
    extends org.springframework.data.repository.Repository<WorkflowTestConfigurationConnection, Long> {

    @Query("""
            SELECT workflow_test_configuration_connection.* FROM workflow_test_configuration_connection
            JOIN workflow_test_configuration ON workflow_test_configuration_connection.workflow_test_configuration_id = workflow_test_configuration.id
            WHERE workflow_test_configuration.workflow_id = :workflowId
            AND workflow_test_configuration_connection.workflow_node_name = :workflowNodeName
        """)
    List<WorkflowTestConfigurationConnection> findByWorkflowIdAndWorkflowNodeName(
        @Param("workflowId") String workflowId, @Param("workflowNodeName") String workflowNodeName);
}
