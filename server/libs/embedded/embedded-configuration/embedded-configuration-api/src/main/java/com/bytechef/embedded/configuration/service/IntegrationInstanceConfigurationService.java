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

package com.bytechef.embedded.configuration.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface IntegrationInstanceConfigurationService {

    IntegrationInstanceConfiguration create(IntegrationInstanceConfiguration integrationInstanceConfiguration);

    void delete(long id);

    boolean isIntegrationInstanceConfigurationEnabled(long integrationInstanceConfigurationId);

    IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(long id);

    List<Long> getIntegrationIds();

    List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations();

    List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(long integrationId);

    List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(List<Long> ids);

    List<IntegrationInstanceConfiguration>
        getIntegrationInstanceConfigurations(Environment environment, Long integrationId, Long tagId);

    IntegrationInstanceConfiguration update(long id, List<Long> tagIds);

    IntegrationInstanceConfiguration update(IntegrationInstanceConfiguration integrationInstanceConfiguration);

    void updateEnabled(long id, boolean enabled);
}
