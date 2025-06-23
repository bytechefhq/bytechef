/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceConfigurationRepository
    extends ListPagingAndSortingRepository<IntegrationInstanceConfiguration, Long>,
    ListCrudRepository<IntegrationInstanceConfiguration, Long>, CustomIntegrationInstanceConfigurationRepository {

    @Query("SELECT integration_instance_configuration.integration_id FROM integration_instance_configuration")
    List<Long> findAllIntegrationIds();

    List<IntegrationInstanceConfiguration> findAllByEnvironmentAndEnabled(int environment, boolean enabled);

    List<IntegrationInstanceConfiguration> findAllByIdIn(List<Long> ids);

    Optional<IntegrationInstanceConfiguration> findByIntegrationIdAndEnvironmentAndEnabled(
        long id, int environment, boolean enabled);
}
