/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import java.util.List;
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
public interface IntegrationInstanceRepository
    extends ListPagingAndSortingRepository<IntegrationInstance, Long>, ListCrudRepository<IntegrationInstance, Long> {

    List<IntegrationInstance> findAllByConnectedUserId(long connectedUserId);

    @Query("""
        SELECT DISTINCT integration_instance.* FROM integration_instance
        JOIN integration_instance_configuration on integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
        JOIN integration on integration_instance_configuration.integration_id = integration.id
        WHERE integration.component_name = :componentName
        AND integration_instance_configuration.environment = :environment
        AND integration_instance.connected_user_id = :connectedUserId
        LIMIT 1
        """)
    List<IntegrationInstance> findAllByConnectedUserIdIdAndComponentNameAndEnvironment(
        @Param("connectedUserId") long connectedUserId, @Param("componentName") String componentName,
        @Param("environment") int environment);

    @Query("""
        SELECT DISTINCT integration_instance.* FROM integration_instance
        JOIN integration_instance_configuration on integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
        WHERE integration_instance_configuration.environment = :environment
        AND integration_instance.connected_user_id = :connectedUserId
        """)
    List<IntegrationInstance> findAllByConnectedUserIdAndEnvironment(
        @Param("connectedUserId") long connectedUserId, @Param("environment") int environment);

    List<IntegrationInstance> findAllByConnectedUserIdIn(List<Long> connectedUserIds);

    List<IntegrationInstance> findAllByConnectedUserIdAndEnabled(long connectedUserId, boolean enabled);

    List<IntegrationInstance> findAllByIntegrationInstanceConfigurationId(long integrationInstanceConfigurationId);

    @Query("""
        SELECT DISTINCT integration_instance.* FROM integration_instance
        JOIN integration_instance_configuration on integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
        JOIN integration_instance_configuration_workflow on integration_instance_configuration.id = integration_instance_configuration_workflow.integration_instance_configuration_id
        WHERE integration_instance_configuration_workflow.workflow_id = :workflowId
        AND integration_instance_configuration.environment = :environment
        AND integration_instance.connected_user_id = :connectedUserId
        """)
    Optional<IntegrationInstance> findByWorkflowIdAndEnvironment(
        @Param("connectedUserId") long connectedUserId, @Param("workflowId") String workflowId,
        @Param("environment") int environment);

    @Query("""
        SELECT DISTINCT integration_instance.* FROM integration_instance
        JOIN integration_instance_configuration on integration_instance.integration_instance_configuration_id = integration_instance_configuration.id
        JOIN integration on integration_instance_configuration.integration_id = integration.id
        WHERE integration.component_name = :componentName
        AND integration_instance_configuration.environment = :environment
        AND integration_instance.connected_user_id = :connectedUserId
        ORDER BY integration_instance.created_date DESC
        LIMIT 1
        """)
    Optional<IntegrationInstance> findFirstByConnectedUserIdIdAndComponentNameAndEnvironment(
        @Param("connectedUserId") long connectedUserId, @Param("componentName") String componentName,
        @Param("environment") int environment);

    @Query("""
        SELECT DISTINCT integration_instance.* FROM integration_instance
        JOIN integration_instance_configuration on integration_instance_configuration_id = integration_instance_configuration.id
        JOIN integration on integration_instance_configuration.integration_id = integration.id
        WHERE integration.component_name in (:componentNames)
        AND integration_instance_configuration.environment = :environment
        AND integration_instance.connected_user_id = :connectedUserId
        ORDER BY integration_instance.created_date DESC
        LIMIT 1
        """)
    Optional<IntegrationInstance> findFirstByConnectedUserIdIdAndComponentNamesAndEnvironment(
        @Param("connectedUserId") long connectedUserId, @Param("componentNames") List<String> componentNames,
        @Param("environment") int environment);
}
