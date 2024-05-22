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

import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationWorkflowRepository
    extends ListCrudRepository<IntegrationWorkflow, Long> {

    int countByIntegrationIdAndIntegrationVersion(long integrationId, int integrationVersion);

    List<IntegrationWorkflow> findAllByIntegrationId(long integrationId);

    List<IntegrationWorkflow> findAllByIntegrationIdAndIntegrationVersion(long integrationId, int integrationVersion);

    Optional<IntegrationWorkflow> findByIntegrationIdAndIntegrationVersionAndWorkflowId(
        long integrationId, int integrationVersion, String workflowId);

    Optional<IntegrationWorkflow> findByWorkflowId(String workflowId);

    @Query("""
            SELECT * FROM integration_workflow
            JOIN integration_instance_configuration ON integration_instance_configuration.integration_id = integration_workflow.integration_id
            AND integration_instance_configuration.integration_version = integration_workflow.integration_version
            JOIN integration_instance ON integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
            WHERE integration_workflow.workflow_reference_code = :workflowReferenceCode
            AND integration_instance.id = :integrationInstanceId
        """)
    Optional<IntegrationWorkflow> findByIntegrationInstanceIdWorkflowReferenceCode(
        long integrationInstanceId, String workflowReferenceCode);
}
