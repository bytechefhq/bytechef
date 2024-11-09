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

import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceWorkflowRepository extends ListCrudRepository<IntegrationInstanceWorkflow, Long> {

    List<IntegrationInstanceWorkflow> findAllByIntegrationInstanceId(long integrationInstanceId);

    @Query("""
        SELECT integration_instance_workflow.* FROM integration_instance_workflow
        JOIN integration_instance_configuration_workflow ON integration_instance_configuration_workflow.id = integration_instance_workflow.integration_instance_configuration_workflow_id
        WHERE integration_instance_workflow.integration_instance_id = :integrationInstanceId
        AND integration_instance_configuration_workflow.workflow_id = :workflowId
        """)
    Optional<IntegrationInstanceWorkflow> findByIntegrationInstanceIdAndWorkflowId(
        @Param("integrationInstanceId") long integrationInstanceId, @Param("workflowId") String workflowId);

    @Modifying
    @Query("""
        DELETE FROM integration_instance_workflow
        WHERE integration_instance_configuration_workflow_id = :integrationInstanceConfigurationWorkflowId
        """)
    void deleteByIntegrationInstanceConfigurationWorkflowId(
        @Param("integrationInstanceConfigurationWorkflowId") long integrationInstanceConfigurationWorkflowId);

}
