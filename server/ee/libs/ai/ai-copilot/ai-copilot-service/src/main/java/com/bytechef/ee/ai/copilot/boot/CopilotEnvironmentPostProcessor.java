/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.boot;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CopilotEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        if (environment.getProperty("bytechef.ai.copilot.enabled", Boolean.class, false)) {
            String provider = environment.getProperty("bytechef.ai.copilot.provider");

            source.put("spring.ai.model.chat", provider);
            source.put("spring.ai.vectorstore.type", "pgvector");
        }

        MapPropertySource mapPropertySource = new MapPropertySource("Custom AI Copilot Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
