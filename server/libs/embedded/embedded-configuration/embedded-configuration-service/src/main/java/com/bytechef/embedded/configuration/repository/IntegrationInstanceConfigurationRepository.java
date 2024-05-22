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

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface IntegrationInstanceConfigurationRepository
    extends ListPagingAndSortingRepository<IntegrationInstanceConfiguration, Long>,
    ListCrudRepository<IntegrationInstanceConfiguration, Long>, CustomIntegrationInstanceConfigurationRepository {

    @Query("SELECT integration_instance_configuration.integration_id FROM integration_instance_configuration")
    List<Long> findAllIntegrationIds();

    @Query("SELECT integration_instance_configuration.integration_id FROM integration_instance_configuration WHERE environment=:environment AND enabled=:enabled")
    List<Long> findAllIntegrationIdsByEnvironmentAndEnabled(
        @Param("environment") int environment, @Param("enabled") boolean enabled);

    List<IntegrationInstanceConfiguration> findAllIntegrationInstanceConfigurationsByIdIn(List<Long> ids);
}
