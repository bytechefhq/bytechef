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
 * Registers the OpenNLP detector when an operator has both enabled it AND configured at least one model.
 *
 * <p>
 * The second condition matters: an enabled-but-empty configuration would otherwise register a detector that can never
 * contribute a span, which the engine would then call on every request for nothing. Absent is clearer than inert.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.guardrails.opennlp", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenNlpGuardrailsProperties.class)
public class OpenNlpGuardrailsConfiguration {

    // @ConditionalOnProperty(name = "entity-models") does not fire for a map-valued property: relaxed binding exposes
    // the map as bytechef.ai.guardrails.opennlp.entity-models.PERSON=..., never as a property literally named
    // entity-models, so the condition's environment.containsProperty lookup always misses and the bean would never
    // register even with a model configured. @ConditionalOnExpression referencing the bound properties bean by name
    // was tried next and also does not work: @EnableConfigurationProperties registers the bean under a generated name
    // (prefix + fully qualified class name), not the simple "openNlpGuardrailsProperties" a hand-written SpEL
    // expression would need to guess. EntityModelsConfiguredCondition sidesteps both problems by binding the map
    // straight from the Environment the same way the properties class itself does, independent of any bean name.
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

    /**
     * Matches only when at least one {@code bytechef.ai.guardrails.opennlp.entity-models.*} entry is bound. Reads
     * straight from the environment via {@link Binder}, the same mechanism
     * {@link OpenNlpGuardrailsProperties#getEntityModels()} relies on, so this does not depend on the properties bean
     * having registered under any particular name.
     */
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
