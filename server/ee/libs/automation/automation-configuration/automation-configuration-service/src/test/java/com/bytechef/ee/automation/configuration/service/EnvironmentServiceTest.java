/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EnvironmentServiceTest {

    private final EnvironmentService environmentService = new EnvironmentServiceImpl();

    @Test
    public void testGetEnvironments() {
        List<Environment> environments = environmentService.getEnvironments();

        assertThat(environments).containsExactly(Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION);
    }
}
