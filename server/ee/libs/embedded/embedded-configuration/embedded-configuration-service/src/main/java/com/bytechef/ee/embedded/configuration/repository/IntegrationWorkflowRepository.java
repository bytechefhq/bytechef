/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
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
public interface IntegrationWorkflowRepository extends ListCrudRepository<IntegrationWorkflow, Long> {

    List<IntegrationWorkflow> findAllByIntegrationId(long integrationId);

    List<IntegrationWorkflow> findAllByIntegrationIdAndIntegrationVersion(long integrationId, int integrationVersion);

    Optional<IntegrationWorkflow> findByIntegrationIdAndIntegrationVersionAndWorkflowId(
        long integrationId, int integrationVersion, String workflowId);

    Optional<IntegrationWorkflow> findByWorkflowId(String workflowId);

    @Query("""
        SELECT integration_workflow.* FROM integration_workflow
        JOIN integration_instance_configuration ON integration_instance_configuration.integration_id = integration_workflow.integration_id
        AND integration_instance_configuration.integration_version = integration_workflow.integration_version
        JOIN integration_instance ON integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
        WHERE integration_workflow.workflow_reference_code = :workflowReferenceCode
        AND integration_instance.id = :integrationInstanceId
        """)
    Optional<IntegrationWorkflow> findByIntegrationInstanceIdAndWorkflowReferenceCode(
        @Param("integrationInstanceId") long integrationInstanceId,
        @Param("workflowReferenceCode") String workflowReferenceCode);

    @Query("""
        SELECT integration_workflow.* FROM integration_workflow
        JOIN integration_instance_configuration ON integration_instance_configuration.integration_id = integration_workflow.integration_id
        AND integration_instance_configuration.integration_version = integration_workflow.integration_version
        JOIN integration_instance ON integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
        WHERE integration_workflow.workflow_reference_code = :workflowReferenceCode
        AND integration_instance_configuration.environment = :environment
        ORDER BY integration_workflow.integration_version DESC
        LIMIT 1
        """)
    Optional<IntegrationWorkflow> findLatestByWorkflowReferenceCodeAndEnvironment(
        @Param("workflowReferenceCode") String workflowReferenceCode, @Param("environment") int environment);

    Optional<IntegrationWorkflow> findLatestIntegrationWorkflowByWorkflowReferenceCode(String workflowReferenceCode);
}
