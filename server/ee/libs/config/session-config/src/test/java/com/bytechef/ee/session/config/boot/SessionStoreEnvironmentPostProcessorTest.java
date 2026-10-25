/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.session.config.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SessionStoreEnvironmentPostProcessorTest {

    private static final String DATA_REDIS_AUTO_CONFIGURATION =
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration";
    private static final String JDBC_SESSION_AUTO_CONFIGURATION =
        "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration";
    private static final String NEO4J_AUTO_CONFIGURATION =
        "org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration";
    private static final String REDIS_SESSION_AUTO_CONFIGURATION =
        "org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration";

    private static final String EXCLUDE = String.join(
        ",", DATA_REDIS_AUTO_CONFIGURATION, JDBC_SESSION_AUTO_CONFIGURATION, REDIS_SESSION_AUTO_CONFIGURATION,
        NEO4J_AUTO_CONFIGURATION);

    private final SessionStoreEnvironmentPostProcessor sessionStoreEnvironmentPostProcessor =
        new SessionStoreEnvironmentPostProcessor();

    @Test
    void testRedisStoreInEeEnablesRedisSessions() {
        StandardEnvironment environment = createEnvironment("ee", "redis");

        sessionStoreEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude"))
            .doesNotContain("DataRedisAutoConfiguration", "SessionDataRedisAutoConfiguration")
            .contains("JdbcSessionAutoConfiguration", "Neo4jAutoConfiguration");
        assertThat(environment.getProperty("spring.session.data.redis.namespace")).isEqualTo("bytechef:session");
    }

    @Test
    void testJdbcStoreInEeEnablesJdbcSessions() {
        StandardEnvironment environment = createEnvironment("ee", "jdbc");

        sessionStoreEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude"))
            .doesNotContain("JdbcSessionAutoConfiguration")
            .contains("DataRedisAutoConfiguration", "SessionDataRedisAutoConfiguration", "Neo4jAutoConfiguration");
        assertThat(environment.getProperty("spring.session.jdbc.table-name")).isEqualTo("public.spring_session");
        assertThat(environment.getProperty("spring.session.jdbc.initialize-schema")).isEqualTo("never");
    }

    @Test
    void testRedisStoreIsNotEnabledOffCoordinators() {
        assertUnchanged(createEnvironment("ee", "redis", "bytechef.coordinator.enabled", "false"));
    }

    @Test
    void testJdbcStoreIsNotEnabledOffCoordinators() {
        assertUnchanged(createEnvironment("ee", "jdbc", "bytechef.coordinator.enabled", "false"));
    }

    @Test
    void testStoreIsEnabledWhenCoordinatorFlagIsTrue() {
        StandardEnvironment environment = createEnvironment("ee", "jdbc", "bytechef.coordinator.enabled", "true");

        sessionStoreEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.session.jdbc.table-name")).isEqualTo("public.spring_session");
    }

    @Test
    void testEditionAndStoreAreCaseInsensitive() {
        StandardEnvironment environment = createEnvironment("EE", "REDIS");

        sessionStoreEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude"))
            .doesNotContain("DataRedisAutoConfiguration", "SessionDataRedisAutoConfiguration")
            .contains("JdbcSessionAutoConfiguration", "Neo4jAutoConfiguration");
        assertThat(environment.getProperty("spring.session.data.redis.namespace")).isEqualTo("bytechef:session");
    }

    @Test
    void testMemoryStoreChangesNothing() {
        assertUnchanged(createEnvironment("ee", "memory"));
    }

    @Test
    void testCeEditionChangesNothing() {
        assertUnchanged(createEnvironment("ce", "redis"));
    }

    @Test
    void testMissingEditionChangesNothing() {
        assertUnchanged(createEnvironment(null, "redis"));
    }

    @Test
    void testUnknownStoreIsLeftToPropertyBinding() {
        assertUnchanged(createEnvironment("ee", "postgres"));
    }

    private void assertUnchanged(StandardEnvironment environment) {
        sessionStoreEnvironmentPostProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.autoconfigure.exclude")).isEqualTo(EXCLUDE);
        assertThat(environment.getProperty("spring.session.data.redis.namespace")).isNull();
        assertThat(environment.getProperty("spring.session.jdbc.table-name")).isNull();
    }

    private static StandardEnvironment createEnvironment(String edition, String store, String... extraProperties) {
        StandardEnvironment environment = new StandardEnvironment();

        Map<String, Object> properties = new HashMap<>();

        if (edition != null) {
            properties.put("bytechef.edition", edition);
        }

        properties.put("bytechef.session.store", store);
        properties.put("spring.autoconfigure.exclude", EXCLUDE);

        for (int i = 0; i < extraProperties.length; i += 2) {
            properties.put(extraProperties[i], extraProperties[i + 1]);
        }

        environment.getPropertySources()
            .addLast(new MapPropertySource("application", properties));

        return environment;
    }
}
