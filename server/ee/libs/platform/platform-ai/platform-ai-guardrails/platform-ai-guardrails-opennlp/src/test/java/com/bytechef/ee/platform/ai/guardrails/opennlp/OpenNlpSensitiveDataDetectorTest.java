/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveKind;
import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveSpan;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Covers {@link OpenNlpSensitiveDataDetector} model loading and {@code detect} — tokenization, character-offset
 * mapping, and the confidence floor.
 *
 * @version ee
 */
class OpenNlpSensitiveDataDetectorTest {

    @Test
    void testNameIsStable() {
        assertThat(detector(Map.of("PERSON", TrainedTestModels.personModelResource())).name())
            .isEqualTo("opennlp-ner");
    }

    @Test
    void testLoadsEveryConfiguredModelAtConstruction() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of(
                "PERSON", TrainedTestModels.personModelResource(),
                "ORGANIZATION", TrainedTestModels.organizationModelResource()));

        assertThat(detector.name()).isEqualTo("opennlp-ner");
    }

    @Test
    void testMissingModelResourceFailsConstruction() {
        assertThatThrownBy(
            () -> detector(Map.of("PERSON", new ClassPathResource("models/does-not-exist.bin"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PERSON");
    }

    @Test
    void testCorruptModelResourceFailsConstruction() {
        assertThatThrownBy(
            () -> detector(Map.of("PERSON", new ByteArrayResource("not a model".getBytes()))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PERSON");
    }

    @Test
    void testRejectsCategoryThatIsNotAValidSpanCategory() {
        assertThatThrownBy(() -> detector(Map.of("lower-case", TrainedTestModels.personModelResource())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lower-case");
    }

    @Test
    void testMapsAMultiTokenEntityToItsCharacterOffsets() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        String text = "Hello Ada Lovelace wrote the note.";

        List<SensitiveSpan> spans = detector.detect(text);

        // Asserts the token-to-character-offset arithmetic, not the model's precision: spans.getFirst() would
        // couple this test to no spurious span preceding "Ada Lovelace" ever being emitted, which is a property of
        // the trained model rather than of the code under test (see TrainedTestModels' own comment — a spurious
        // span DID precede it, until a fourth training sample was added). anySatisfy proves the arithmetic is
        // right for whichever span covers the entity, regardless of what else the model also reports.
        assertThat(spans)
            .as("a span covering the multi-token entity")
            .anySatisfy(candidate -> {
                assertThat(text.substring(candidate.start(), candidate.end())).isEqualTo("Ada Lovelace");
                assertThat(candidate.category()).isEqualTo("PERSON");
                assertThat(candidate.kind()).isEqualTo(SensitiveKind.PII);
            });
    }

    @Test
    void testSpansBelowMinConfidenceAreDropped() {
        Map<String, Resource> models = Map.of("PERSON", TrainedTestModels.personModelResource());
        String text = "Ada Lovelace wrote the note.";

        List<SensitiveSpan> permissive = new OpenNlpSensitiveDataDetector(models, null, 0.0).detect(text);

        assertThat(permissive).isNotEmpty();

        // Also covers the deleted testSpansCarryTheModelProbabilityAsConfidence: this is the one place a span's
        // confidence value is inspected for real (clampProbability already guarantees the [0.0, 1.0] range, so
        // asserting only that range would pass against a hardcoded 0.5).
        double observedConfidence = permissive.getFirst()
            .confidence();

        assertThat(observedConfidence).isBetween(0.0, 1.0);

        // The threshold is derived from the run rather than pinned at an unreachable constant (an earlier version
        // used 1.01, above anything clampProbability can produce, which proves only "a threshold above the maximum
        // drops everything" and pins nothing an operator would actually set). "<" in the production comparison is
        // strict, so a threshold EQUAL to the observed confidence must still keep the span...
        List<SensitiveSpan> atObservedConfidence =
            new OpenNlpSensitiveDataDetector(models, null, observedConfidence).detect(text);

        assertThat(atObservedConfidence).isNotEmpty();

        // ...and a threshold just above it must drop the span, pinning "<" rather than "<=" at the boundary.
        List<SensitiveSpan> justAboveObservedConfidence =
            new OpenNlpSensitiveDataDetector(models, null, Math.nextUp(observedConfidence)).detect(text);

        assertThat(justAboveObservedConfidence).isEmpty();
    }

    @Test
    void testTextWithNoEntitiesYieldsNoSpans() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        assertThat(detector.detect("the note was filed yesterday")).isEmpty();
    }

    @Test
    void testEmptyTextYieldsNoSpans() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        assertThat(detector.detect("")).isEmpty();
    }

    @Test
    void testDetectsBothConfiguredCategoriesInOneText() {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of(
                "PERSON", TrainedTestModels.personModelResource(),
                "ORGANIZATION", TrainedTestModels.organizationModelResource()));

        String text = "Ada Lovelace wrote the note. Acme Corp shipped it.";

        List<SensitiveSpan> spans = detector.detect(text);

        assertThat(spans)
            .extracting(SensitiveSpan::category)
            .contains("PERSON", "ORGANIZATION");
    }

    @Test
    void testIsNotStreamSafe() {
        assertThat(detector(Map.of("PERSON", TrainedTestModels.personModelResource())).streamSafe())
            .isFalse();
    }

    @Test
    void testOneFailingModelDoesNotSuppressAnother() throws Exception {
        OpenNlpSensitiveDataDetector detector = detectorWithEntityModels(
            new OpenNlpSensitiveDataDetector.EntityModel("PERSON", null),
            new OpenNlpSensitiveDataDetector.EntityModel(
                "ORGANIZATION", loadModel(TrainedTestModels.organizationModelResource())));

        List<SensitiveSpan> spans = detector.detect("Acme Corp shipped it.");

        assertThat(spans).isNotEmpty();
        assertThat(spans).allMatch(span -> "ORGANIZATION".equals(span.category()));
    }

    @Test
    void testConcurrentDetectionReturnsCorrectSpans() throws Exception {
        OpenNlpSensitiveDataDetector detector = detector(
            Map.of("PERSON", TrainedTestModels.personModelResource()));

        String text = "Hello Ada Lovelace wrote the note.";

        ExecutorService executorService = Executors.newFixedThreadPool(8);

        try {
            List<Future<String>> futures = new ArrayList<>();

            for (int attempt = 0; attempt < 200; attempt++) {
                futures.add(executorService.submit(() -> {
                    List<SensitiveSpan> spans = detector.detect(text);

                    if (spans.isEmpty()) {
                        return "<none>";
                    }

                    SensitiveSpan span = spans.getFirst();

                    return text.substring(span.start(), span.end());
                }));
            }

            for (Future<String> future : futures) {
                assertThat(future.get()).isEqualTo("Ada Lovelace");
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private static OpenNlpSensitiveDataDetector detector(Map<String, Resource> entityModelResources) {
        return new OpenNlpSensitiveDataDetector(entityModelResources, null, 0.0);
    }

    /**
     * Builds a detector directly from already-loaded {@link OpenNlpSensitiveDataDetector.EntityModel}s via the
     * package-private test-seam constructor, bypassing resource loading so a model can be deliberately broken (a
     * {@code null} {@code TokenNameFinderModel}) to exercise per-model failure isolation without a corrupt model file.
     */
    private static OpenNlpSensitiveDataDetector detectorWithEntityModels(
        OpenNlpSensitiveDataDetector.EntityModel... entityModels) {

        return new OpenNlpSensitiveDataDetector(List.of(entityModels), () -> SimpleTokenizer.INSTANCE, 0.0);
    }

    private static TokenNameFinderModel loadModel(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            return new TokenNameFinderModel(inputStream);
        }
    }
}
