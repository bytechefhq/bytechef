/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
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
public class IntegrationInstanceEnvironmentResolver implements ResourceEnvironmentResolver {

    private final IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;
    private final IntegrationInstanceRepository integrationInstanceRepository;

    @SuppressFBWarnings("EI")
    public IntegrationInstanceEnvironmentResolver(
        IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository,
        IntegrationInstanceRepository integrationInstanceRepository) {

        this.integrationInstanceConfigurationRepository = integrationInstanceConfigurationRepository;
        this.integrationInstanceRepository = integrationInstanceRepository;
    }

    @Override
    public String resourceType() {
        return "IntegrationInstance";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return integrationInstanceRepository.findById(number.longValue())
            .map(IntegrationInstance::getIntegrationInstanceConfigurationId)
            .flatMap(integrationInstanceConfigurationRepository::findById)
            .map(IntegrationInstanceConfiguration::getEnvironment);
    }
}
