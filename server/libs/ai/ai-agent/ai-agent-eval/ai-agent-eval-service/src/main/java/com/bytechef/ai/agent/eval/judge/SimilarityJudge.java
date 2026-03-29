/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.agent.eval.judge;

import java.util.HashMap;
import java.util.Map;
import org.springaicommunity.judge.DeterministicJudge;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;
import org.springaicommunity.judge.score.NumericalScore;

/**
 * A deterministic judge that measures text similarity between the agent output and an expected output using either edit
 * distance or cosine similarity.
 */
public class SimilarityJudge extends DeterministicJudge {

    private final String algorithm;
    private final String expectedOutput;
    private final double threshold;

    public SimilarityJudge(String expectedOutput, double threshold, String algorithm) {
        super("Similarity", "Measures text similarity between agent output and expected output");

        this.expectedOutput = expectedOutput;
        this.threshold = threshold;
        this.algorithm = algorithm;
    }

    @Override
    public Judgment judge(JudgmentContext context) {
        String output = context.agentOutput()
            .orElse("");

        String resolvedExpectedOutput = expectedOutput;

        if (resolvedExpectedOutput == null) {
            Object metadataExpectedOutput = context.metadata()
                .get("expectedOutput");

            if (metadataExpectedOutput instanceof String metadataExpectedOutputString) {
                resolvedExpectedOutput = metadataExpectedOutputString;
            }
        }

        if (resolvedExpectedOutput == null) {
            return Judgment.error("No expected output provided",
                new IllegalArgumentException("expectedOutput is null"));
        }

        double similarity;

        if ("COSINE".equals(algorithm)) {
            similarity = calculateCosineSimilarity(output, resolvedExpectedOutput);
        } else {
            similarity = calculateEditDistanceSimilarity(output, resolvedExpectedOutput);
        }

        JudgmentStatus status = (similarity >= threshold) ? JudgmentStatus.PASS : JudgmentStatus.FAIL;

        String reasoning = String.format(
            "Similarity score %.4f (%s) is %s the threshold %.4f", similarity, algorithm,
            (similarity >= threshold) ? "at or above" : "below", threshold);

        return Judgment.builder()
            .score(NumericalScore.normalized(similarity))
            .status(status)
            .reasoning(reasoning)
            .build();
    }

    private double calculateCosineSimilarity(String text1, String text2) {
        Map<String, Integer> vector1 = buildWordFrequencyVector(text1);
        Map<String, Integer> vector2 = buildWordFrequencyVector(text2);

        double dotProduct = 0;
        double magnitude1 = 0;
        double magnitude2 = 0;

        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            int frequency1 = entry.getValue();

            magnitude1 += (double) frequency1 * frequency1;

            Integer frequency2 = vector2.get(entry.getKey());

            if (frequency2 != null) {
                dotProduct += (double) frequency1 * frequency2;
            }
        }

        for (int frequency : vector2.values()) {
            magnitude2 += (double) frequency * frequency;
        }

        if ((magnitude1 == 0) || (magnitude2 == 0)) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private double calculateEditDistanceSimilarity(String text1, String text2) {
        int maxLength = Math.max(text1.length(), text2.length());

        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(text1, text2);

        return 1.0 - ((double) distance / maxLength);
    }

    private Map<String, Integer> buildWordFrequencyVector(String text) {
        Map<String, Integer> vector = new HashMap<>();

        String[] words = text.toLowerCase()
            .split("\\s+");

        for (String word : words) {
            if (!word.isEmpty()) {
                vector.merge(word, 1, Integer::sum);
            }
        }

        return vector;
    }

    private int levenshteinDistance(String text1, String text2) {
        int length1 = text1.length();
        int length2 = text2.length();

        int[][] distanceMatrix = new int[length1 + 1][length2 + 1];

        for (int index = 0; index <= length1; index++) {
            distanceMatrix[index][0] = index;
        }

        for (int index = 0; index <= length2; index++) {
            distanceMatrix[0][index] = index;
        }

        for (int index1 = 1; index1 <= length1; index1++) {
            for (int index2 = 1; index2 <= length2; index2++) {
                int cost = (text1.charAt(index1 - 1) == text2.charAt(index2 - 1)) ? 0 : 1;

                distanceMatrix[index1][index2] = Math.min(
                    Math.min(distanceMatrix[index1 - 1][index2] + 1, distanceMatrix[index1][index2 - 1] + 1),
                    distanceMatrix[index1 - 1][index2 - 1] + cost);
            }
        }

        return distanceMatrix[length1][length2];
    }

}
