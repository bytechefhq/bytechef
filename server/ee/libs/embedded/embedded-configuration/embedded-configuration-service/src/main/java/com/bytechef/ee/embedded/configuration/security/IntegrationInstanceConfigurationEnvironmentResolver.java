/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationEnvironmentResolver implements ResourceEnvironmentResolver {

    private final IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationEnvironmentResolver(
        IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository) {

        this.integrationInstanceConfigurationRepository = integrationInstanceConfigurationRepository;
    }

    @Override
    public String resourceType() {
        return "IntegrationInstanceConfiguration";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return integrationInstanceConfigurationRepository.findById(number.longValue())
            .map(IntegrationInstanceConfiguration::getEnvironment);
    }
}
