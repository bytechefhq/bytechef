/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromptComplexityScorerConfigurationTest {

    @Test
    void testDefaultsToTheDeterministicScorer() {
        try (AnnotationConfigApplicationContext context = contextWith(
            Map.of("bytechef.edition", "ee", "bytechef.ai.gateway.enabled", "true"))) {
            assertThat(context.getBeanNamesForType(PromptComplexityScorer.class)).hasSize(1);
            assertThat(context.getBean(PromptComplexityScorer.class)).isInstanceOf(
                DeterministicPromptComplexityScorer.class);
        }
    }

    @Test
    void testExplicitDeterministicSelectsTheDeterministicScorer() {
        try (AnnotationConfigApplicationContext context = contextWith(
            Map.of(
                "bytechef.edition", "ee", "bytechef.ai.gateway.enabled", "true",
                "bytechef.ai.gateway.prompt-complexity-scorer", "deterministic"))) {
            assertThat(context.getBeanNamesForType(PromptComplexityScorer.class)).hasSize(1);
            assertThat(context.getBean(PromptComplexityScorer.class)).isInstanceOf(
                DeterministicPromptComplexityScorer.class);
        }
    }

    @Test
    void testOpenNlpSelectsTheOpenNlpScorer() {
        try (AnnotationConfigApplicationContext context = contextWith(
            Map.of(
                "bytechef.edition", "ee", "bytechef.ai.gateway.enabled", "true",
                "bytechef.ai.gateway.prompt-complexity-scorer", "opennlp"))) {
            assertThat(context.getBeanNamesForType(PromptComplexityScorer.class)).hasSize(1);
            assertThat(context.getBean(PromptComplexityScorer.class)).isInstanceOf(
                OpenNlpPromptComplexityScorer.class);
        }
    }

    @Test
    void testBacksOffWhenTheGatewayIsDisabled() {
        try (AnnotationConfigApplicationContext context = contextWith(Map.of("bytechef.edition", "ee"))) {
            assertThat(context.getBeanNamesForType(PromptComplexityScorer.class)).isEmpty();
        }
    }

    @Test
    void testBacksOffOnACommunityEdition() {
        try (AnnotationConfigApplicationContext context = contextWith(
            Map.of("bytechef.edition", "ce", "bytechef.ai.gateway.enabled", "true"))) {
            assertThat(context.getBeanNamesForType(PromptComplexityScorer.class)).isEmpty();
        }
    }

    private static AnnotationConfigApplicationContext contextWith(Map<String, Object> properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        ConfigurableEnvironment environment = context.getEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();

        propertySources.addFirst(new MapPropertySource("promptComplexityScorerConfigurationTest", properties));

        context.register(PromptComplexityScorerConfiguration.class);
        context.refresh();

        return context;
    }
}
