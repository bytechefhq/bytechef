/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import java.util.HashMap;
import java.util.Map;
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
public class AiCopilotEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        if (environment.getProperty("bytechef.ai.copilot.enabled", Boolean.class, false)) {
            source.put("spring.ai.model.chat", environment.getProperty("bytechef.ai.copilot.chat.provider"));
            source.put("spring.ai.model.embedding", environment.getProperty("bytechef.ai.copilot.embedding.provider"));
            source.put("spring.ai.vectorstore.type", "pgvector");
        }

        MapPropertySource mapPropertySource = new MapPropertySource("Custom AI Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
