/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
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

        log.info(
            "OpenNLP sensitive-data detector configured {} model(s) with a minimum confidence of {}; PII redaction "
                + "must also be enabled for these spans to be redacted",
            entityModels.size(), minConfidence);
    }

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
                log.warn(
                    "OpenNLP model for category '{}' failed; continuing without its spans", entityModel.category(),
                    exception);
            }
        }

        return spans;
    }

    private List<SensitiveSpan> findSpans(EntityModel entityModel, String[] tokens, Span[] tokenPositions) {
        NameFinderME nameFinder = new NameFinderME(entityModel.model());

        Span[] entities = nameFinder.find(tokens);

        List<SensitiveSpan> spans = new ArrayList<>(entities.length);

        for (Span entity : entities) {
            if (entity.getStart() < 0 || entity.getEnd() > tokenPositions.length ||
                entity.getStart() >= entity.getEnd()) {

                throw new IllegalStateException(
                    "OpenNLP model for category " + entityModel.category() + " returned an out-of-range token span");
            }

            double confidence = clampProbability(entity.getProb());

            if (confidence < minConfidence) {
                continue;
            }

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
