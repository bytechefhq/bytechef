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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;
import org.springaicommunity.judge.score.NumericalScore;

/**
 * @author ByteChef
 */
class SimilarityJudgeTest {

    @Test
    void testEditDistanceIdenticalText() {
        SimilarityJudge judge = new SimilarityJudge("hello world", 0.8, "EDIT_DISTANCE");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());

        NumericalScore score = (NumericalScore) judgment.score();

        assertEquals(1.0, score.value(), 0.001);
    }

    @Test
    void testEditDistanceSimilarText() {
        SimilarityJudge judge = new SimilarityJudge("hello world", 0.5, "EDIT_DISTANCE");

        Judgment judgment = judge.judge(buildContext("hello worl"));

        assertEquals(JudgmentStatus.PASS, judgment.status());

        NumericalScore score = (NumericalScore) judgment.score();

        assertTrue(score.value() > 0.5);
    }

    @Test
    void testEditDistanceDifferentTextBelowThreshold() {
        SimilarityJudge judge = new SimilarityJudge("hello world", 0.9, "EDIT_DISTANCE");

        Judgment judgment = judge.judge(buildContext("completely different text"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCosineSimilarityIdenticalText() {
        SimilarityJudge judge = new SimilarityJudge("the quick brown fox", 0.8, "COSINE");

        Judgment judgment = judge.judge(buildContext("the quick brown fox"));

        assertEquals(JudgmentStatus.PASS, judgment.status());

        NumericalScore score = (NumericalScore) judgment.score();

        assertEquals(1.0, score.value(), 0.001);
    }

    @Test
    void testCosineSimilarityOverlappingWords() {
        SimilarityJudge judge = new SimilarityJudge("the quick brown fox", 0.3, "COSINE");

        Judgment judgment = judge.judge(buildContext("the quick red dog"));

        assertEquals(JudgmentStatus.PASS, judgment.status());

        NumericalScore score = (NumericalScore) judgment.score();

        assertTrue(score.value() > 0.3);
    }

    @Test
    void testCosineSimilarityCompletelyDifferent() {
        SimilarityJudge judge = new SimilarityJudge("hello world", 0.5, "COSINE");

        Judgment judgment = judge.judge(buildContext("completely different text"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());

        NumericalScore score = (NumericalScore) judgment.score();

        assertEquals(0.0, score.value(), 0.001);
    }

    @Test
    void testNullExpectedOutputWithMetadataFallback() {
        SimilarityJudge judge = new SimilarityJudge(null, 0.8, "EDIT_DISTANCE");

        JudgmentContext context = JudgmentContext.builder()
            .goal("test")
            .agentOutput("hello world")
            .metadata(Map.of("expectedOutput", "hello world"))
            .build();

        Judgment judgment = judge.judge(context);

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testNullExpectedOutputWithoutMetadataReturnsError() {
        SimilarityJudge judge = new SimilarityJudge(null, 0.8, "EDIT_DISTANCE");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    @Test
    void testEditDistanceEmptyStrings() {
        SimilarityJudge judge = new SimilarityJudge("", 0.5, "EDIT_DISTANCE");

        Judgment judgment = judge.judge(buildContext(""));

        assertEquals(JudgmentStatus.PASS, judgment.status());

        NumericalScore score = (NumericalScore) judgment.score();

        assertEquals(1.0, score.value(), 0.001);
    }

    private JudgmentContext buildContext(String agentOutput) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput(agentOutput)
            .build();
    }

}
