/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationRepository
    extends ListPagingAndSortingRepository<Integration, Long>, ListCrudRepository<Integration, Long>,
    CustomIntegrationRepository {

    @Query("""
        SELECT integration.* FROM integration
        JOIN integration_instance_configuration ON integration.id = integration_instance_configuration.integration_id
        JOIN integration_instance ON integration_instance_configuration.id = integration_instance.integration_instance_configuration_id
        WHERE integration_instance.id = :integrationInstanceId
        """)
    Optional<Integration> findByIntegrationInstanceId(@Param("integrationInstanceId") long integrationInstanceId);

    @Query("""
        SELECT integration.* FROM integration
        JOIN integration_instance_configuration ON integration.id = integration_instance_configuration.integration_id
        WHERE integration_instance_configuration.id = :integrationInstanceConfigurationId
        """)
    Optional<Integration> findByIntegrationInstanceConfigurationId(
        @Param("integrationInstanceConfigurationId") long integrationInstanceConfigurationId);

    @Query("""
        SELECT integration.* FROM integration
        JOIN integration_workflow ON integration.id = integration_workflow.integration_id
        WHERE integration_workflow.workflow_id = :workflowId
        """)
    Optional<Integration> findByWorkflowId(@Param("workflowId") String workflowId);
}
