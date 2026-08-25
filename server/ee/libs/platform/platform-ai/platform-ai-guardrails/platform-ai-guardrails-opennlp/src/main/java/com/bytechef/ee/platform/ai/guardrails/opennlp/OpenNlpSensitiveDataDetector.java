/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveDataDetector;
import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveKind;
import com.bytechef.ee.platform.ai.guardrails.detector.SensitiveSpan;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * Detects unstructured personally-identifiable data — person names, organizations, locations — using operator-supplied
 * Apache OpenNLP name-finder models. This module ships none: Apache distributes no NER models, so the detector is inert
 * until an operator configures at least one.
 *
 * <p>
 * <b>Models are loaded eagerly</b> in this constructor. A missing, unreadable, or corrupt model fails application
 * startup rather than being caught later by the engine's fail-open detector policy, which would hand the operator a
 * guardrail that silently protects nothing.
 * </p>
 *
 * <p>
 * <b>Thread safety.</b> {@link TokenNameFinderModel} is thread-safe and is held for the process lifetime;
 * {@code NameFinderME} is NOT, and is therefore constructed per {@code detect} call (see Task 3). Caching the finder
 * instead is the natural-looking optimization and is wrong — its state is per-document, so concurrent calls interleave
 * and produce spans at positions that never held an entity.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class OpenNlpSensitiveDataDetector implements SensitiveDataDetector {

    private static final Logger log = LoggerFactory.getLogger(OpenNlpSensitiveDataDetector.class);

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final List<EntityModel> entityModels;
    private final double minConfidence;
    private final Supplier<Tokenizer> tokenizerSupplier;

    public OpenNlpSensitiveDataDetector(
        Map<String, Resource> entityModelResources, @Nullable Resource tokenizerModelResource, double minConfidence) {

        this.entityModels = loadEntityModels(entityModelResources);
        this.minConfidence = minConfidence;
        this.tokenizerSupplier = loadTokenizerSupplier(tokenizerModelResource);

        // Deliberately phrased as what was loaded, not as confirmation the detector is active: every span this
        // detector reports is SensitiveKind.PII, so it also requires the workspace's PII-redaction toggle to be on
        // before it redacts anything. This log line alone must not read as "guardrail is now protecting requests".
        log.info(
            "OpenNLP sensitive-data detector configured {} model(s) with a minimum confidence of {}; PII redaction "
                + "must also be enabled for these spans to be redacted",
            entityModels.size(), minConfidence);
    }

    /**
     * Test seam only. Bypasses resource loading so a test can hand the detector already-constructed
     * {@link EntityModel}s directly — for example a model deliberately broken to exercise per-model failure isolation.
     * Not a production entry point.
     */
    OpenNlpSensitiveDataDetector(List<EntityModel> entityModels, Supplier<Tokenizer> tokenizerSupplier,
        double minConfidence) {

        this.entityModels = List.copyOf(entityModels);
        this.minConfidence = minConfidence;
        this.tokenizerSupplier = tokenizerSupplier;
    }

    @Override
    public String name() {
        return "opennlp-ner";
    }

    /**
     * Named-entity recognition is not local: run over a bounded lookahead window that starts mid-sentence, it gives
     * different and worse answers than over the whole text. Returning {@code false} keeps this detector out of the
     * streaming redactor rather than letting it contribute unreliable spans there. Streamed completions therefore get
     * regex redaction only; batch response scanning and all request-direction scanning still cover NER.
     */
    @Override
    public boolean streamSafe() {
        return false;
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        Tokenizer tokenizer = tokenizerSupplier.get();

        Span[] tokenPositions = tokenizer.tokenizePos(text);

        if (tokenPositions.length == 0) {
            return List.of();
        }

        String[] tokens = new String[tokenPositions.length];

        for (int index = 0; index < tokenPositions.length; index++) {
            tokens[index] = tokenPositions[index].getCoveredText(text)
                .toString();
        }

        List<SensitiveSpan> spans = new ArrayList<>();

        for (EntityModel entityModel : entityModels) {
            try {
                spans.addAll(findSpans(entityModel, tokens, tokenPositions));
            } catch (RuntimeException exception) {
                // Contain the failure to the model that caused it. The engine's SensitiveDataRedactor discards a
                // detector's ENTIRE batch when detect() throws, so letting this propagate would silently cost every
                // other category its spans too.
                log.warn(
                    "OpenNLP model for category '{}' failed; continuing without its spans", entityModel.category(),
                    exception);
            }
        }

        return spans;
    }

    private List<SensitiveSpan> findSpans(EntityModel entityModel, String[] tokens, Span[] tokenPositions) {
        // NameFinderME is not thread-safe and this detector is shared across every concurrent request, so a fresh one
        // is built per call. It wraps the already-parsed model, so construction is cheap. This also removes the
        // clearAdaptiveData() obligation a cached finder would carry, whose omission would leak one request's entity
        // context into the next.
        NameFinderME nameFinder = new NameFinderME(entityModel.model());

        Span[] entities = nameFinder.find(tokens);

        List<SensitiveSpan> spans = new ArrayList<>(entities.length);

        for (Span entity : entities) {
            if (entity.getStart() < 0 || entity.getEnd() > tokenPositions.length ||
                entity.getStart() >= entity.getEnd()) {

                throw new IllegalStateException(
                    "OpenNLP model for category " + entityModel.category() + " returned an out-of-range token span");
            }

            // find() already reconstructs each returned span with its own per-span probability (the mean of the
            // underlying token probabilities across the span) via getProb(), so that is the source of truth here —
            // not a second nameFinder.probs(entities) call, which would recompute the identical value from the
            // finder's mutable bestSequence field and is fragile to reorder against a future refactor.
            double confidence = clampProbability(entity.getProb());

            if (confidence < minConfidence) {
                continue;
            }

            // getEnd() is exclusive, so the entity's last token is at getEnd() - 1.
            int start = tokenPositions[entity.getStart()].getStart();
            int end = tokenPositions[entity.getEnd() - 1].getEnd();

            spans.add(new SensitiveSpan(SensitiveKind.PII, entityModel.category(), start, end, confidence));
        }

        return spans;
    }

    private static double clampProbability(double probability) {
        if (!Double.isFinite(probability)) {
            return 0.0;
        }

        return Math.min(1.0, Math.max(0.0, probability));
    }

    private static List<EntityModel> loadEntityModels(Map<String, Resource> entityModelResources) {
        List<EntityModel> loaded = new ArrayList<>(entityModelResources.size());

        for (Map.Entry<String, Resource> entry : entityModelResources.entrySet()) {
            String category = entry.getKey();

            if (!CATEGORY_PATTERN.matcher(category)
                .matches()) {

                throw new IllegalArgumentException(
                    "OpenNLP entity-model category must match [A-Z][A-Z0-9_]*, got: " + category);
            }

            loaded.add(new EntityModel(category, loadEntityModel(category, entry.getValue())));
        }

        return List.copyOf(loaded);
    }

    private static TokenNameFinderModel loadEntityModel(String category, Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new TokenNameFinderModel(inputStream);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                "Could not load the OpenNLP model configured for category " + category + " from " +
                    resource.getDescription(),
                exception);
        }
    }

    private static Supplier<Tokenizer> loadTokenizerSupplier(@Nullable Resource tokenizerModelResource) {
        if (tokenizerModelResource == null) {
            return () -> SimpleTokenizer.INSTANCE;
        }

        try (InputStream inputStream = tokenizerModelResource.getInputStream()) {
            TokenizerModel tokenizerModel = new TokenizerModel(inputStream);

            // TokenizerME is not thread-safe, so a fresh one is built per call, exactly as NameFinderME is.
            return () -> new TokenizerME(tokenizerModel);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                "Could not load the OpenNLP tokenizer model from " + tokenizerModelResource.getDescription(),
                exception);
        }
    }

    record EntityModel(String category, TokenNameFinderModel model) {
    }
}
