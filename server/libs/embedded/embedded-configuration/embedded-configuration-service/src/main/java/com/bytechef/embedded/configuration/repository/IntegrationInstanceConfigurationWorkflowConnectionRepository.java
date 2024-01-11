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

package com.bytechef.embedded.configuration.repository;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceConfigurationWorkflowConnectionRepository
    extends org.springframework.data.repository.Repository<IntegrationInstanceConfigurationWorkflowConnection, Long> {

    List<IntegrationInstanceConfigurationWorkflowConnection> findByConnectionId(long connectionId);

    @Query("""
            SELECT integration_instance_configuration_workflow_connection.* FROM integration_instance_configuration_workflow_connection
            JOIN integration_instance_configuration_workflow ON integration_instance_configuration_workflow_connection.integration_instance_configuration_workflow_id = integration_instance_configuration_workflow.id
            WHERE integration_instance_configuration_workflow.integration_instance_configuration_id = :projectInstanceId
            AND integration_instance_configuration_workflow.workflow_id = :workflowId
            AND integration_instance_configuration_workflow_connection.workflow_noden_name = :workflowNodeName
        """)
    List<IntegrationInstanceConfigurationWorkflowConnection>
        findAllByIntegrationInstanceIdAndWorkflowIdAndOperationName(
            @Param("projectInstanceId") long projectInstanceId, @Param("workflowId") String workflowId,
            @Param("workflowNodeName") String workflowNodeName);

    @Query("""
            SELECT integration_instance_configuration_workflow_connection.* FROM integration_instance_configuration_workflow_connection
            JOIN integration_instance_configuration_workflow ON integration_instance_configuration_workflow_connection.integration_instance_configuration_workflow_id = integration_instance_configuration_workflow.id
            WHERE integration_instance_configuration_workflow.integration_instance_configuration_id = :integrationInstanceConfigurationId
            AND integration_instance_configuration_workflow.workflow_id = :workflowId
            AND integration_instance_configuration_workflow_connection.workflow_noden_name = :workflowNodeName
            AND integration_instance_configuration_workflow_connection.key = :key
        """)
    Optional<IntegrationInstanceConfigurationWorkflowConnection>
        findByIntegrationInstanceIdAndWorkflowIdAndOperationNameAndKey(
            @Param("integrationInstanceConfigurationId") long integrationInstanceConfigurationId,
            @Param("workflowId") String workflowId, @Param("workflowNodeName") String workflowNodeName,
            @Param("key") String key);

    @Query("""
            SELECT integration_instance_configuration_workflow_connection.* FROM integration_instance_configuration_workflow_connection
            JOIN integration_instance_configuration_workflow ON integration_instance_configuration_workflow_connection.integration_instance_configuration_workflow_id = integration_instance_configuration_workflow.id
            WHERE integration_instance_configuration_workflow.integration_instance_configuration_id = :integrationInstanceConfigurationId
            AND integration_instance_configuration_workflow.workflow_id = :workflowId
            AND integration_instance_configuration_workflow_connection.workflow_node_name = :workflowNodeName
            AND integration_instance_configuration_workflow_connection.key = :workflowConnectionKey
        """)
    Optional<IntegrationInstanceConfigurationWorkflowConnection>
        findByIntegrationInstanceConfigurationIdAndWorkflowIdAndWorkflowNodeNameAndKey(
            @Param("integrationInstanceConfigurationId") long integrationInstanceConfigurationId,
            @Param("workflowId") String workflowId, @Param("workflowNodeName") String workflowNodeName,
            @Param("workflowConnectionKey") String workflowConnectionKey);
}
