/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ai.copilot.config;

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
public class AiCopilotEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        if (!environment.getProperty("bytechef.ai.copilot.enabled", Boolean.class, false)) {
            source.put("spring.ai.chat.client.enabled", false);

            if (!Objects.equals(
                environment.getProperty("bytechef.ai.copilot.provider", String.class, "openai"), "openai")) {

                source.put(
                    "spring.autoconfigure.exclude",
                    environment.getProperty("spring.autoconfigure.exclude") +
                        ",org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration" +
                        ",org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration");
            }

            // TODO Add support for other providers
        }

        MapPropertySource mapPropertySource = new MapPropertySource("Custom AI Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
