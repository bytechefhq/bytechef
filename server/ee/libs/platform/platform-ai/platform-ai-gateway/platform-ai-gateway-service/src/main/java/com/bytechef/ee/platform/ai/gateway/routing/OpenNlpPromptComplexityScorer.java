/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.TrainingParameters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class OpenNlpPromptComplexityScorer implements PromptComplexityScorer {

    private static final int CUTOFF = 1;
    private static final int ITERATIONS = 100;
    private static final String LANGUAGE = "en";
    private static final String COMPLEX_LABEL = "COMPLEX";
    private static final String EXEMPLARS_RESOURCE = "/ai-gateway/routing/prompt-complexity-exemplars.jsonl";

    private final DocumentCategorizerME categorizer;
    private final int complexIndex;

    public OpenNlpPromptComplexityScorer() {
        this(loadExemplars());
    }

    OpenNlpPromptComplexityScorer(List<LabelledPrompt> exemplars) {
        List<DocumentSample> samples = new ArrayList<>();

        for (LabelledPrompt exemplar : exemplars) {
            samples.add(new DocumentSample(exemplar.label(), tokenize(exemplar.text())));
        }

        TrainingParameters trainingParameters = new TrainingParameters();

        trainingParameters.put(TrainingParameters.CUTOFF_PARAM, CUTOFF);
        trainingParameters.put(TrainingParameters.ITERATIONS_PARAM, ITERATIONS);

        try (ObjectStream<DocumentSample> sampleStream = ObjectStreamUtils.createObjectStream(samples)) {
            DoccatModel model = DocumentCategorizerME.train(
                LANGUAGE, sampleStream, trainingParameters, new DoccatFactory());

            this.categorizer = new DocumentCategorizerME(model);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to train the OpenNLP prompt complexity categoriser", exception);
        }

        this.complexIndex = categorizer.getIndex(COMPLEX_LABEL);
    }

    @Override
    public double score(AiGatewayChatCompletionRequest request) {
        String[] tokens = tokenize(concatenateContent(request));

        if (tokens.length == 0) {
            return 0.0;
        }

        double[] outcomes = categorizer.categorize(tokens);

        return Math.max(0.0, Math.min(outcomes[complexIndex], 1.0));
    }

    private static String concatenateContent(AiGatewayChatCompletionRequest request) {
        StringBuilder builder = new StringBuilder();

        for (AiGatewayChatMessage message : request.messages()) {
            String content = message.content();

            if (content != null) {
                builder.append(content)
                    .append(' ');
            }
        }

        return builder.toString();
    }

    private static String[] tokenize(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }

        return SimpleTokenizer.INSTANCE.tokenize(text);
    }

    private static List<LabelledPrompt> loadExemplars() {
        List<LabelledPrompt> exemplars = new ArrayList<>();

        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        try (InputStream inputStream = OpenNlpPromptComplexityScorer.class.getResourceAsStream(
            EXEMPLARS_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException(
                    "Prompt complexity exemplar resource not found: " + EXEMPLARS_RESOURCE);
            }

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();

                    if (trimmed.isEmpty()) {
                        continue;
                    }

                    JsonNode node = objectMapper.readTree(trimmed);

                    exemplars.add(new LabelledPrompt(node.path("text")
                        .asString(),
                        node.path("label")
                            .asString()));
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read prompt complexity exemplars", exception);
        }

        return exemplars;
    }

    record LabelledPrompt(String text, String label) {
    }
}
