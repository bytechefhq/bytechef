/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.guardrails.opennlp", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenNlpGuardrailsProperties.class)
public class OpenNlpGuardrailsConfiguration {

    @Bean
    @Conditional(EntityModelsConfiguredCondition.class)
    OpenNlpSensitiveDataDetector openNlpSensitiveDataDetector(
        OpenNlpGuardrailsProperties openNlpGuardrailsProperties, ResourceLoader resourceLoader) {

        Map<String, Resource> entityModelResources = new LinkedHashMap<>();

        Map<String, String> configuredModels = openNlpGuardrailsProperties.getEntityModels();

        for (Map.Entry<String, String> entry : configuredModels.entrySet()) {
            entityModelResources.put(entry.getKey(), resourceLoader.getResource(entry.getValue()));
        }

        return new OpenNlpSensitiveDataDetector(
            entityModelResources, tokenizerResource(openNlpGuardrailsProperties, resourceLoader),
            openNlpGuardrailsProperties.getMinConfidence());
    }

    private static @Nullable Resource tokenizerResource(
        OpenNlpGuardrailsProperties openNlpGuardrailsProperties, ResourceLoader resourceLoader) {

        String tokenizerModel = openNlpGuardrailsProperties.getTokenizerModel();

        if (!StringUtils.hasText(tokenizerModel)) {
            return null;
        }

        return resourceLoader.getResource(tokenizerModel);
    }

    static final class EntityModelsConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Map<String, String> entityModels = Binder.get(context.getEnvironment())
                .bind("bytechef.ai.guardrails.opennlp.entity-models", Bindable.mapOf(String.class, String.class))
                .orElseGet(Map::of);

            return !entityModels.isEmpty();
        }
    }
}
