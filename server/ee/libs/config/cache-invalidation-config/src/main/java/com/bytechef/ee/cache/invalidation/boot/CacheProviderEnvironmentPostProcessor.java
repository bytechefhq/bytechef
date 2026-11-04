/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation.boot;

import com.bytechef.message.broker.redis.control.DataRedisAutoConfigurationEnabler;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Turns Redis auto-configuration on when EE keeps its shared caches in Redis.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CacheProviderEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!"ee".equalsIgnoreCase(environment.getProperty("bytechef.edition")) ||
            !"redis".equalsIgnoreCase(environment.getProperty("bytechef.cache.provider"))) {

            return;
        }

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(
            new MapPropertySource(
                "Custom Redis Cache Config", DataRedisAutoConfigurationEnabler.getProperties(environment)));
    }
}
