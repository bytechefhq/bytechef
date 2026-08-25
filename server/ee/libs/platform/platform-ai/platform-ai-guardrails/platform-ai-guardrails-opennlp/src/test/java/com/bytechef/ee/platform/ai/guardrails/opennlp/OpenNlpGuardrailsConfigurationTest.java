/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataDetector;
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
        // CE edition: @ConditionalOnEEVersion must keep the detector out of the context regardless of the
        // opennlp-specific enabled/entity-models values. Without this guard, a CE deployment that set
        // enabled=true with a bad or good model path would try to load OpenNLP models for an engine (AiGuardrails)
        // that does not exist in that build.
        contextRunner
            .withPropertyValues(
                "bytechef.edition=ce",
                "bytechef.ai.guardrails.opennlp.enabled=true",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON=" + personModelPath())
            .run(context -> assertThat(context).doesNotHaveBean(SensitiveDataDetector.class));
    }

    @Test
    void testCeStartupSucceedsEvenWithABadModelPath() {
        // The failure this guards against: without @ConditionalOnEEVersion, a CE deployment that enabled the
        // opennlp detector with a broken model path would still try to eager-load it and fail startup for an
        // engine (AiGuardrails) that CE does not even carry. In CE the bean must never be attempted at all, so
        // startup succeeds regardless of how broken the configured model path is.
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
        // No bytechef.edition property at all: @ConditionalOnEEVersion's missing-value semantics must be
        // "absent", not "default to ee".
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

                // hasFailed() alone would pass for ANY startup failure, including one unrelated to model loading.
                // Pin the actual cause: OpenNlpSensitiveDataDetector.loadEntityModel's eager-load IllegalStateException
                // must be somewhere in the chain, and its message must name the broken category. The category is what
                // matters here, not the file path, because that is what tells an operator which of their configured
                // models is broken.
                Throwable modelLoadFailure = findCauseOfType(context.getStartupFailure(), IllegalStateException.class);

                assertThat(modelLoadFailure)
                    .as("eager model-load failure from OpenNlpSensitiveDataDetector.loadEntityModel")
                    .isNotNull();

                assertThat(modelLoadFailure)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("category PERSON");
            });
    }

    /**
     * Walks the cause chain looking for an exception of {@code type}. Spring wraps a {@code @Bean} factory method's
     * thrown exception in a {@code BeanInstantiationException} and then a {@code BeanCreationException} before it
     * reaches the context's startup failure, so asserting only on the outermost exception would not distinguish the
     * eager model-load failure this test exists to pin from any other cause of context startup failure.
     */
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

    /**
     * Writes the in-memory trained model to a temp file, because the configuration resolves models from resource
     * STRINGS and a ByteArrayResource has no string form.
     */
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
