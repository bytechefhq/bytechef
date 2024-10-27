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

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceRepository
    extends ListPagingAndSortingRepository<IntegrationInstance, Long>, ListCrudRepository<IntegrationInstance, Long> {

    List<IntegrationInstance> findAllByConnectedUserId(long connectedUserId);

    List<IntegrationInstance> findAllByConnectedUserIdIn(List<Long> connectedUserIds);

    List<IntegrationInstance> findAllByConnectedUserIdAndEnabled(long connectedUserId, boolean enabled);

    @Query("""
        SELECT DISTINCT * FROM integration_instance
        JOIN integration_instance_configuration on integration_instance_configuration_id = integration_instance_configuration.id
        JOIN integration_instance_configuration_workflow on integration_instance_configuration.id = integration_instance_configuration_workflow.integration_instance_configuration_id
        WHERE integration_instance_configuration_workflow.workflow_id = :workflowId
        AND integration_instance_configuration.environment = :environment
        AND integration_instance.connected_user_id = :connectedUserId
        """)
    Optional<IntegrationInstance> findByWorkflowIdAndEnvironment(
        @Param("connectedUserId") long connectedUserId, @Param("workflowId") String workflowId,
        @Param("environment") int environment);
}
