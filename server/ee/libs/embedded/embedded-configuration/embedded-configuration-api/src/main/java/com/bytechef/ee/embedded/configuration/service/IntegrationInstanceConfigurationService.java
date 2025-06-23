/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceConfigurationService {

    IntegrationInstanceConfiguration create(IntegrationInstanceConfiguration integrationInstanceConfiguration);

    void delete(long id);

    boolean isIntegrationInstanceConfigurationEnabled(long integrationInstanceConfigurationId);

    IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(long id);

    IntegrationInstanceConfiguration getIntegrationIntegrationInstanceConfiguration(
        long id, Environment environment, boolean enabled);

    List<Long> getIntegrationIds();

    List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations();

    List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(long integrationId);

    List<IntegrationInstanceConfiguration>
        getIntegrationInstanceConfigurations(Environment environment, boolean enabled);

    List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(List<Long> ids);

    List<IntegrationInstanceConfiguration>
        getIntegrationInstanceConfigurations(Environment environment, Long integrationId, Long tagId);

    IntegrationInstanceConfiguration update(long id, List<Long> tagIds);

    IntegrationInstanceConfiguration update(IntegrationInstanceConfiguration integrationInstanceConfiguration);

    void updateEnabled(long id, boolean enabled);

}
