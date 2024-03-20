/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.discovery.redis.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisDiscoveryServiceProviderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        if (Objects.equals(environment.getProperty("bytechef.discovery-service.provider", String.class), "redis")) {
            source.put("spring.cloud.redis.enabled", true);
        } else {
            source.put("spring.cloud.redis.enabled", false);
        }

        MapPropertySource mapPropertySource = new MapPropertySource("Custom Spring Cloud Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
