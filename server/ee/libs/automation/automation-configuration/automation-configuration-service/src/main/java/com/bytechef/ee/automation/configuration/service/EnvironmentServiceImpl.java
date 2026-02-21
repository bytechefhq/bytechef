/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class EnvironmentServiceImpl implements EnvironmentService {

    private final Environment environment;

    public EnvironmentServiceImpl(ApplicationProperties applicationProperties) {
        this.environment = applicationProperties.getEnvironment();
    }

    @Override
    public List<Environment> getEnvironments() {
        if (environment != null) {
            return List.of(environment);
        }

        return Arrays.asList(Environment.values());
    }
}
