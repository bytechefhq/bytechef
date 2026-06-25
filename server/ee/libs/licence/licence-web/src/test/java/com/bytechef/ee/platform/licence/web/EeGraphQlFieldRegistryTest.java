/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EeGraphQlFieldRegistryTest {

    @Test
    void testEeFieldDetectedFromQueryMapping() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.getEnvironment()
            .getPropertySources()
            .addFirst(new MapPropertySource("test", Map.of("bytechef.edition", "ee")));
        context.register(EeGraphQlController.class);
        context.refresh();

        EeGraphQlFieldRegistry registry = new EeGraphQlFieldRegistry(context);

        assertThat(registry.isEeField("licence")).isTrue();
        assertThat(registry.isEeField("someCeField")).isFalse();

        context.close();
    }

    @Test
    void testCeControllerFieldNotRegistered() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(CeGraphQlController.class);
        context.refresh();

        EeGraphQlFieldRegistry registry = new EeGraphQlFieldRegistry(context);

        assertThat(registry.isEeField("ceField")).isFalse();

        context.close();
    }

    @Test
    void testEeFieldDetectedWhenBothConditionalOnEEVersionAndConditionalOnPropertyPresent() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.getEnvironment()
            .getPropertySources()
            .addFirst(new MapPropertySource("test", Map.of(
                "bytechef.edition", "ee",
                "bytechef.test.feature.enabled", "true")));
        context.register(EeAndPropertyGraphQlController.class);
        context.refresh();

        EeGraphQlFieldRegistry registry = new EeGraphQlFieldRegistry(context);

        assertThat(registry.isEeField("eeFeatureField")).isTrue();

        context.close();
    }

    @Controller
    @ConditionalOnEEVersion
    static class EeGraphQlController {

        @QueryMapping
        public String licence() {
            return "licence";
        }
    }

    @Controller
    static class CeGraphQlController {

        @QueryMapping
        public String ceField() {
            return "ceField";
        }
    }

    @Controller
    @ConditionalOnEEVersion
    @ConditionalOnProperty(prefix = "bytechef.test.feature", name = "enabled", havingValue = "true")
    static class EeAndPropertyGraphQlController {

        @QueryMapping
        public String eeFeatureField() {
            return "eeFeatureField";
        }
    }
}
