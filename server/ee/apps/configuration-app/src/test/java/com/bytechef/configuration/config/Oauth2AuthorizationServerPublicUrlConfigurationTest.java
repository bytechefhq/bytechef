/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.configuration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class Oauth2AuthorizationServerPublicUrlConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class, Oauth2AuthorizationServerPublicUrlConfiguration.class);

    @Test
    void testStartupFailsWhenTheAuthorizationServerIsEnabledWithoutAPublicUrl() {
        applicationContextRunner
            .withPropertyValues("bytechef.oauth2.authorization-server.enabled=true", "bytechef.public-url=")
            .run(context -> assertThat(context).hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining("bytechef.public-url must be set"));
    }

    @Test
    void testStartupSucceedsWhenTheAuthorizationServerIsEnabledWithAPublicUrl() {
        applicationContextRunner
            .withPropertyValues(
                "bytechef.oauth2.authorization-server.enabled=true",
                "bytechef.public-url=https://bytechef.example.test")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void testStartupSucceedsWithoutAPublicUrlWhenTheAuthorizationServerIsDisabled() {
        applicationContextRunner
            .withPropertyValues("bytechef.oauth2.authorization-server.enabled=false", "bytechef.public-url=")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class PropertiesConfiguration {
    }
}
