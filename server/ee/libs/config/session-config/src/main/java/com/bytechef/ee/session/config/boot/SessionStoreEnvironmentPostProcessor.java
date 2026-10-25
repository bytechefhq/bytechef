/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.session.config.boot;

import com.bytechef.message.broker.redis.control.DataRedisAutoConfigurationEnabler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Enables the Redis or JDBC HTTP session store selected by {@code bytechef.session.store} in the Enterprise Edition by
 * lifting the exclusion of the matching Spring Session auto-configuration.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SessionStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";
    private static final String JDBC_SESSION_AUTO_CONFIGURATION =
        "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration";
    private static final String REDIS_SESSION_AUTO_CONFIGURATION =
        "org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!"ee".equalsIgnoreCase(environment.getProperty("bytechef.edition"))) {
            return;
        }

        Map<String, Object> source = new HashMap<>();

        String store = environment.getProperty("bytechef.session.store");

        if ("redis".equalsIgnoreCase(store)) {
            source.putAll(DataRedisAutoConfigurationEnabler.getProperties(environment));

            String exclude = (String) source.get(AUTOCONFIGURE_EXCLUDE);

            if (exclude == null) {
                exclude = environment.getProperty(AUTOCONFIGURE_EXCLUDE);
            }

            putExclude(source, exclude, REDIS_SESSION_AUTO_CONFIGURATION);

            source.put("spring.session.data.redis.namespace", "bytechef:session");
        } else if ("jdbc".equalsIgnoreCase(store)) {
            putExclude(source, environment.getProperty(AUTOCONFIGURE_EXCLUDE), JDBC_SESSION_AUTO_CONFIGURATION);

            source.put("spring.session.jdbc.table-name", "public.spring_session");
            source.put("spring.session.jdbc.initialize-schema", "never");

            if ("false".equalsIgnoreCase(environment.getProperty("bytechef.coordinator.enabled"))) {
                source.put("spring.session.jdbc.cleanup-cron", "-");
            }
        }

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(new MapPropertySource("Custom Session Store Config", source));
    }

    private static void putExclude(Map<String, Object> source, String exclude, String autoConfiguration) {
        if (exclude == null) {
            return;
        }

        source.put(
            AUTOCONFIGURE_EXCLUDE,
            Arrays.stream(exclude.split(","))
                .map(String::trim)
                .filter(className -> !className.isEmpty() && !className.equals(autoConfiguration))
                .collect(Collectors.joining(",")));
    }
}
