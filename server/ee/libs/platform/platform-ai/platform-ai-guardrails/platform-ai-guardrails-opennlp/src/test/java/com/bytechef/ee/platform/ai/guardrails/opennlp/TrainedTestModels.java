/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import opennlp.tools.namefind.BioCodec;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamUtils;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.model.ModelUtil;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Trains throwaway OpenNLP name-finder models in memory, so the adapter can be tested end to end without shipping a
 * model file or touching the network. The models are deliberately tiny — they recognise only the names they were
 * trained on — which is all that is needed to exercise tokenization, offset mapping, and thresholding.
 *
 * @version ee
 */
final class TrainedTestModels {

    private TrainedTestModels() {
    }

    static Resource personModelResource() {
        return modelResource(
            "person",
            sample(new String[] {
                "Ada", "Lovelace", "wrote", "the", "note", "."
            }, 0, 2),
            sample(new String[] {
                "Alan", "Turing", "read", "the", "note", "."
            }, 0, 2),
            sample(new String[] {
                "Ada", "Lovelace", "and", "Alan", "Turing", "met", "."
            }, 0, 2, 3, 5),
            // Without a sentence-initial word that is NOT itself an entity, every training sentence starts its
            // entity at token 0 — the model then overfits to "capitalized first token" as a person-start signal and
            // misfires on decoy prefixes such as "Hello". This sample is otherwise identical to the first one, with
            // one prepended token, so it teaches sentence-initial position alone without confusing the model about
            // anything else.
            sample(new String[] {
                "Hello", "Ada", "Lovelace", "wrote", "the", "note", "."
            }, 1, 3));
    }

    static Resource organizationModelResource() {
        return modelResource(
            "organization",
            sample(new String[] {
                "Acme", "Corp", "shipped", "it", "."
            }, 0, 2),
            sample(new String[] {
                "Globex", "shipped", "it", "."
            }, 0, 1),
            sample(new String[] {
                "Acme", "Corp", "bought", "Globex", "."
            }, 0, 2, 3, 4));
    }

    /**
     * Builds one training sample. Span bounds are TOKEN indices, given as start/end pairs, end exclusive.
     */
    private static NameSample sample(String[] tokens, int... spanBounds) {
        List<Span> spans = new ArrayList<>();

        for (int index = 0; index < spanBounds.length; index += 2) {
            spans.add(new Span(spanBounds[index], spanBounds[index + 1]));
        }

        return new NameSample(tokens, spans.toArray(new Span[0]), true);
    }

    private static Resource modelResource(String type, NameSample... samples) {
        TrainingParameters trainingParameters = ModelUtil.createDefaultTrainingParameters();

        trainingParameters.put(TrainingParameters.CUTOFF_PARAM, 0);
        trainingParameters.put(TrainingParameters.ITERATIONS_PARAM, 150);

        try (ObjectStream<NameSample> sampleStream = ObjectStreamUtils.createObjectStream(samples)) {
            TokenNameFinderModel model = NameFinderME.train(
                "eng", type, sampleStream, trainingParameters,
                TokenNameFinderFactory.create(null, null, Collections.emptyMap(), new BioCodec()));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            model.serialize(outputStream);

            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not train the test model for type " + type, exception);
        }
    }
}
