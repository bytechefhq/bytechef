/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceConfigurationWorkflowRepository
    extends ListCrudRepository<IntegrationInstanceConfigurationWorkflow, Long> {

    @Query("""
            SELECT DISTINCT integration_instance_configuration_workflow.* FROM integration_instance_configuration_workflow
            JOIN integration_instance_configuration_workflow_connection ON integration_instance_configuration_workflow.id = integration_instance_configuration_workflow_connection.integration_instance_configuration_workflow_id
            WHERE integration_instance_configuration_workflow_connection.connection_id = :connectionId
        """)
    List<IntegrationInstanceConfigurationWorkflow> findAllByConnectionId(@Param("connectionId") long connectionId);

    List<IntegrationInstanceConfigurationWorkflow> findAllByIntegrationInstanceConfigurationId(
        long integrationInstanceConfigurationId);

    List<IntegrationInstanceConfigurationWorkflow> findAllByIntegrationInstanceConfigurationIdIn(
        List<Long> integrationInstanceConfigurationIds);

    Optional<IntegrationInstanceConfigurationWorkflow>
        findByIntegrationInstanceConfigurationIdAndWorkflowId(
            long integrationInstanceConfigurationId, String workflowId);
}
