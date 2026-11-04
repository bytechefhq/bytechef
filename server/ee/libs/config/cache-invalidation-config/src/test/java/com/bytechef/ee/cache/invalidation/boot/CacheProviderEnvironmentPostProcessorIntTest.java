/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CacheProviderEnvironmentPostProcessorIntTest {

    private static final String DATA_REDIS_AUTO_CONFIGURATION =
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration";
    private static final String OTHER_AUTO_CONFIGURATION =
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withInitializer(context -> new CacheProviderEnvironmentPostProcessor()
            .postProcessEnvironment(context.getEnvironment(), new SpringApplication()))
        .withPropertyValues(
            "spring.autoconfigure.exclude=" + DATA_REDIS_AUTO_CONFIGURATION + "," + OTHER_AUTO_CONFIGURATION);

    @Test
    void testLiftsRedisExclusionInEeWithRedisProvider() {
        contextRunner
            .withPropertyValues("bytechef.edition=ee", "bytechef.cache.provider=redis")
            .run(context -> assertThat(context.getEnvironment()
                .getProperty("spring.autoconfigure.exclude"))
                    .isEqualTo(OTHER_AUTO_CONFIGURATION));
    }

    @Test
    void testLiftsRedisExclusionOnNonCoordinatorNode() {
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ee", "bytechef.cache.provider=redis", "bytechef.coordinator.enabled=false")
            .run(context -> assertThat(context.getEnvironment()
                .getProperty("spring.autoconfigure.exclude"))
                    .isEqualTo(OTHER_AUTO_CONFIGURATION));
    }

    @Test
    void testLiftsRedisExclusionWithUpperCaseValues() {
        contextRunner
            .withPropertyValues("bytechef.edition=EE", "bytechef.cache.provider=REDIS")
            .run(context -> assertThat(context.getEnvironment()
                .getProperty("spring.autoconfigure.exclude"))
                    .isEqualTo(OTHER_AUTO_CONFIGURATION));
    }

    @Test
    void testKeepsRedisExclusionWithCaffeineProvider() {
        contextRunner
            .withPropertyValues("bytechef.edition=ee", "bytechef.cache.provider=caffeine")
            .run(context -> assertThat(context.getEnvironment()
                .getProperty("spring.autoconfigure.exclude"))
                    .contains(DATA_REDIS_AUTO_CONFIGURATION));
    }

    @Test
    void testKeepsRedisExclusionInCe() {
        contextRunner
            .withPropertyValues("bytechef.edition=ce", "bytechef.cache.provider=redis")
            .run(context -> assertThat(context.getEnvironment()
                .getProperty("spring.autoconfigure.exclude"))
                    .contains(DATA_REDIS_AUTO_CONFIGURATION));
    }
}
