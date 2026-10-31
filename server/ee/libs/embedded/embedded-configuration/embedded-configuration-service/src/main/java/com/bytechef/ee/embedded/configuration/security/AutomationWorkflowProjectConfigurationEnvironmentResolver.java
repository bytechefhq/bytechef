/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectConfigurationRepository;
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
public class AutomationWorkflowProjectConfigurationEnvironmentResolver implements ResourceEnvironmentResolver {

    private final AutomationWorkflowProjectConfigurationRepository automationWorkflowProjectConfigurationRepository;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectConfigurationEnvironmentResolver(
        AutomationWorkflowProjectConfigurationRepository automationWorkflowProjectConfigurationRepository) {

        this.automationWorkflowProjectConfigurationRepository = automationWorkflowProjectConfigurationRepository;
    }

    @Override
    public String resourceType() {
        return "AutomationWorkflowProjectConfiguration";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        Environment[] environments = Environment.values();

        return automationWorkflowProjectConfigurationRepository.findById(number.longValue())
            .map(AutomationWorkflowProjectConfiguration::getEnvironmentId)
            .filter(environmentId -> environmentId >= 0 && environmentId < environments.length)
            .map(environmentId -> environments[(int) (long) environmentId]);
    }
}
