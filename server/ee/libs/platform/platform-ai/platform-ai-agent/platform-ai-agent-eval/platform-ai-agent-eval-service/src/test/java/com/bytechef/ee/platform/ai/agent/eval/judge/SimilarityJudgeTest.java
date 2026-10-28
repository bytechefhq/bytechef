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

package com.bytechef.ee.platform.ai.agent.eval.judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.markpollack.judge.completion.CompletionEvidence;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author ByteChef
 */
class SimilarityJudgeTest {

    @Test
    void testEditDistanceIdenticalText() {
        SimilarityJudge judge =
            new SimilarityJudge(() -> buildEvidence("hello world"), "hello world", 0.8, "EDIT_DISTANCE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());

        assertEquals(1.0, judgment.score(), 0.001);
    }

    @Test
    void testEditDistanceSimilarText() {
        SimilarityJudge judge =
            new SimilarityJudge(() -> buildEvidence("hello worl"), "hello world", 0.5, "EDIT_DISTANCE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());

        assertTrue(judgment.score() > 0.5);
    }

    @Test
    void testEditDistanceDifferentTextBelowThreshold() {
        SimilarityJudge judge =
            new SimilarityJudge(() -> buildEvidence("completely different text"), "hello world", 0.9, "EDIT_DISTANCE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCosineSimilarityIdenticalText() {
        SimilarityJudge judge =
            new SimilarityJudge(() -> buildEvidence("the quick brown fox"), "the quick brown fox", 0.8, "COSINE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());

        assertEquals(1.0, judgment.score(), 0.001);
    }

    @Test
    void testCosineSimilarityOverlappingWords() {
        SimilarityJudge judge =
            new SimilarityJudge(() -> buildEvidence("the quick red dog"), "the quick brown fox", 0.3, "COSINE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());

        assertTrue(judgment.score() > 0.3);
    }

    @Test
    void testCosineSimilarityCompletelyDifferent() {
        SimilarityJudge judge =
            new SimilarityJudge(() -> buildEvidence("completely different text"), "hello world", 0.5, "COSINE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());

        assertEquals(0.0, judgment.score(), 0.001);
    }

    @Test
    void testNullExpectedOutputWithMetadataFallback() {
        CompletionEvidence evidence = CompletionEvidence.builder()
            .request("test")
            .response("hello world")
            .metadata(Map.of("expectedOutput", "hello world"))
            .build();

        SimilarityJudge judge = new SimilarityJudge(() -> evidence, null, 0.8, "EDIT_DISTANCE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testNullExpectedOutputWithoutMetadataReturnsError() {
        SimilarityJudge judge = new SimilarityJudge(() -> buildEvidence("hello world"), null, 0.8, "EDIT_DISTANCE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    @Test
    void testEditDistanceEmptyStrings() {
        SimilarityJudge judge = new SimilarityJudge(() -> buildEvidence(""), "", 0.5, "EDIT_DISTANCE");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());

        assertEquals(1.0, judgment.score(), 0.001);
    }

    private CompletionEvidence buildEvidence(String agentOutput) {
        return CompletionEvidence.builder()
            .request("test")
            .response(agentOutput)
            .build();
    }

}
