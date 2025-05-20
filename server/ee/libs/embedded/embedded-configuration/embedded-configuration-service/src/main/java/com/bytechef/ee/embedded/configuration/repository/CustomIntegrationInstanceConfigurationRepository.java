/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomIntegrationInstanceConfigurationRepository {

    List<IntegrationInstanceConfiguration> findAllIntegrationInstanceConfigurations(
        Integer environment, Long projectId, Long tagId);
}
