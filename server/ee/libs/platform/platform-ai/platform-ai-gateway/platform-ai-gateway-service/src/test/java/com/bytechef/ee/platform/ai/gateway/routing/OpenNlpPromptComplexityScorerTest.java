/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenNlpPromptComplexityScorerTest {

    private static final List<OpenNlpPromptComplexityScorer.LabelledPrompt> TRAINING = List.of(
        new OpenNlpPromptComplexityScorer.LabelledPrompt("What is the capital of France?", "SIMPLE"),
        new OpenNlpPromptComplexityScorer.LabelledPrompt("Translate hello into Spanish.", "SIMPLE"),
        new OpenNlpPromptComplexityScorer.LabelledPrompt("List the days of the week.", "SIMPLE"),
        new OpenNlpPromptComplexityScorer.LabelledPrompt(
            "Prove that the square root of two is irrational and explain the contradiction.", "COMPLEX"),
        new OpenNlpPromptComplexityScorer.LabelledPrompt(
            "Design a fault tolerant queue and justify the trade offs you make.", "COMPLEX"),
        new OpenNlpPromptComplexityScorer.LabelledPrompt(
            "Explain why the diagonalisation argument establishes undecidability.", "COMPLEX"));

    private static AiGatewayChatCompletionRequest request(String text) {
        return new AiGatewayChatCompletionRequest(
            "gpt-4", List.of(new AiGatewayChatMessage(AiGatewayChatRole.USER, text)), null, null, null, false, null,
            null, null, null);
    }

    @Test
    void testLoadsCorpusAndScoresComplexPromptAboveSimple() {
        OpenNlpPromptComplexityScorer scorer = new OpenNlpPromptComplexityScorer();

        double complexScore = scorer.score(
            request(
                "Design a fault-tolerant distributed consensus protocol and prove that it satisfies safety and "
                    + "liveness under network partitions."));
        double simpleScore = scorer.score(request("What is the capital of Germany?"));

        assertThat(complexScore).isGreaterThan(simpleScore);
        assertThat(complexScore).isBetween(0.0, 1.0);
        assertThat(simpleScore).isBetween(0.0, 1.0);
    }

    @Test
    void testScoreIsWithinUnitInterval() {
        OpenNlpPromptComplexityScorer scorer = new OpenNlpPromptComplexityScorer(TRAINING);

        assertThat(scorer.score(request("What is the capital of Spain?"))).isBetween(0.0, 1.0);
        assertThat(scorer.score(request("Prove that there are infinitely many primes."))).isBetween(0.0, 1.0);
    }

    @Test
    void testComplexTrainingVocabularyScoresAboveSimple() {
        OpenNlpPromptComplexityScorer scorer = new OpenNlpPromptComplexityScorer(TRAINING);

        double complexScore = scorer.score(request("Prove that the contradiction establishes irrationality."));
        double simpleScore = scorer.score(request("What is the capital of Spain?"));

        assertThat(complexScore).isGreaterThan(simpleScore);
    }

    @Test
    void testEmptyPromptDoesNotThrow() {
        OpenNlpPromptComplexityScorer scorer = new OpenNlpPromptComplexityScorer(TRAINING);

        assertThat(scorer.score(request(""))).isBetween(0.0, 1.0);
    }

    @Test
    void testNullContentDoesNotThrow() {
        OpenNlpPromptComplexityScorer scorer = new OpenNlpPromptComplexityScorer(TRAINING);

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            "bakeoff", List.of(new AiGatewayChatMessage(AiGatewayChatRole.USER, null)), null, null, null, false,
            null, null, null, null);

        assertThat(scorer.score(request)).isBetween(0.0, 1.0);
    }
}
