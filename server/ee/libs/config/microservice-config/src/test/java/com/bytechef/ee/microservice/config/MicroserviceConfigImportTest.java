/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.microservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @version ee
 */
class MicroserviceConfigImportTest {

    @Test
    void testAppFileOverridesSharedFile() {
        assertThat(environment().getProperty("docs.documentation-url")).isEqualTo("https://app.example");
    }

    @Test
    void testSharedFileLoads() {
        assertThat(environment().getProperty("docs.report-bug-url"))
            .isEqualTo("https://github.com/bytechefhq/bytechef/issues");
    }

    @Test
    void testProfileVariantsLoadForBothImports() {
        ConfigurableEnvironment environment = environment("dev");

        assertThat(environment.getProperty("test.marker")).isEqualTo("dev");
        assertThat(environment.getProperty("logging.level.com.bytechef")).isEqualTo("DEBUG");
    }

    private ConfigurableEnvironment environment(String... profiles) {
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(TestConfiguration.class)
            .web(WebApplicationType.NONE)
            .profiles(profiles)
            .properties(
                "spring.config.name=none",
                "spring.config.import=classpath:config/microservice.yml,classpath:config/test-app.yml");

        try (ConfigurableApplicationContext applicationContext = springApplicationBuilder.run()) {
            return applicationContext.getEnvironment();
        }
    }

    @Configuration
    static class TestConfiguration {
    }
}
