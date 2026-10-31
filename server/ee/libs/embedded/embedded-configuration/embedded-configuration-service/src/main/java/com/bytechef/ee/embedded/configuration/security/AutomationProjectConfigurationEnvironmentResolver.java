/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectConfigurationRepository;
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
public class AutomationProjectConfigurationEnvironmentResolver implements ResourceEnvironmentResolver {

    private final AutomationProjectConfigurationRepository automationProjectConfigurationRepository;

    @SuppressFBWarnings("EI")
    public AutomationProjectConfigurationEnvironmentResolver(
        AutomationProjectConfigurationRepository automationProjectConfigurationRepository) {

        this.automationProjectConfigurationRepository = automationProjectConfigurationRepository;
    }

    @Override
    public String resourceType() {
        return "AutomationProjectConfiguration";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        Environment[] environments = Environment.values();

        return automationProjectConfigurationRepository.findById(number.longValue())
            .map(AutomationProjectConfiguration::getEnvironmentId)
            .filter(environmentId -> environmentId >= 0 && environmentId < environments.length)
            .map(environmentId -> environments[(int) (long) environmentId]);
    }
}
