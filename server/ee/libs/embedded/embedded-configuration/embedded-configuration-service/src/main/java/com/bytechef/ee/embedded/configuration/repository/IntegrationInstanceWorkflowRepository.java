/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
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

    List<IntegrationInstanceWorkflow> findAllByIntegrationInstanceIdIn(List<Long> integrationInstanceIds);
}
