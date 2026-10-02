/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.Resource;

/**
 * @version ee
 */
class OpenNlpGuardrailsConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(OpenNlpGuardrailsConfiguration.class);

    @Test
    void testNoDetectorWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ee",
                "bytechef.ai.guardrails.opennlp.enabled=false",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testNoDetectorWhenEnabledButNoModelsConfigured() {
        contextRunner
            .withPropertyValues("bytechef.edition=ee", "bytechef.ai.guardrails.opennlp.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testDetectorRegisteredWhenEnabledWithAModel() {
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ee",
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> {
                assertThat(context).hasSingleBean(SensitiveDataDetector.class);
                assertThat(context.getBean(SensitiveDataDetector.class)
                    .name()).isEqualTo("opennlp-ner");
            });
    }

    @Test
    void testNoDetectorInCeEvenWhenEnabledWithAModel() {
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ce",
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testCeStartupSucceedsEvenWithABadModelPath() {
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ce",
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=file:/does/not/exist.bin")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(SensitiveDataDetector.class);
            });
    }

    @Test
    void testNoDetectorWhenEditionPropertyMissing() {
        contextRunner
            .withPropertyValues(
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testBadModelPathFailsTheContext() {
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ee",
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=file:/does/not/exist.bin")
            .run(context -> {
                assertThat(context).hasFailed();

                Throwable modelLoadFailure = findCauseOfType(context.getStartupFailure(), IllegalStateException.class);

                assertThat(modelLoadFailure)
                    .as("eager model-load failure from OpenNlpSensitiveDataDetector.loadEntityModel")
                    .isNotNull();

                assertThat(modelLoadFailure)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("category PERSON");
            });
    }

    private static @Nullable Throwable findCauseOfType(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;

        while (current != null) {
            if (type.isInstance(current)) {
                return current;
            }

            current = current.getCause();
        }

        return null;
    }

    private static String personModelPath() {
        try {
            Resource resource = TrainedTestModels.personModelResource();

            Path path = Files.createTempFile("bytechef-opennlp-person", ".bin");

            Files.write(path, resource.getInputStream()
                .readAllBytes());

            path.toFile()
                .deleteOnExit();

            return "file:" + path;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not materialize the test model", exception);
        }
    }
}
