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
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceConfigurationWorkflowRepository
    extends ListCrudRepository<IntegrationInstanceConfigurationWorkflow, Long> {

    List<IntegrationInstanceConfigurationWorkflow> findAllByIntegrationInstanceConfigurationId(
        long integrationInstanceConfigurationId);

    List<IntegrationInstanceConfigurationWorkflow> findAllByIntegrationInstanceConfigurationIdIn(
        List<Long> integrationInstanceConfigurationIds);

    Optional<IntegrationInstanceConfigurationWorkflow>
        findByIntegrationInstanceConfigurationIdAndWorkflowId(
            long integrationInstanceConfigurationId, String workflowId);
}
