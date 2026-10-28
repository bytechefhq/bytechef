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

package com.bytechef.ee.platform.ai.agent.eval.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentEvalResultConclusion;
import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentJudgeVerdictStatus;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.verdict.Verdict;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentEvalJuryTest {

    @Test
    void testAllJudgesPassConcludesPass() {
        Verdict verdict = AiAgentEvalJury.vote(
            List.of(named("first", Judgment.pass("ok")), named("second", Judgment.pass("ok"))));

        AiAgentEvalResultConclusion conclusion = AiAgentEvalJury.toConclusion(verdict);

        assertEquals(AiAgentEvalResultConclusion.PASS, conclusion);
        assertEquals(1.0, AiAgentEvalJury.toScore(verdict, conclusion));
    }

    @Test
    void testOneFailingJudgeConcludesFail() {
        Verdict verdict = AiAgentEvalJury.vote(
            List.of(named("first", Judgment.pass("ok")), named("second", Judgment.fail("wrong"))));

        AiAgentEvalResultConclusion conclusion = AiAgentEvalJury.toConclusion(verdict);

        assertEquals(AiAgentEvalResultConclusion.FAIL, conclusion);
        assertEquals(0.5, AiAgentEvalJury.toScore(verdict, conclusion));
    }

    @Test
    void testJudgeErrorConcludesInconclusiveWithoutScore() {
        Verdict verdict = AiAgentEvalJury.vote(
            List.of(named("first", Judgment.pass("ok")), named("second", Judgment.error("unparseable reply"))));

        AiAgentEvalResultConclusion conclusion = AiAgentEvalJury.toConclusion(verdict);

        assertEquals(AiAgentEvalResultConclusion.INCONCLUSIVE, conclusion);
        assertNull(AiAgentEvalJury.toScore(verdict, conclusion));
        assertEquals(
            AiAgentJudgeVerdictStatus.ERROR,
            AiAgentEvalJury.toStatus(
                AiAgentEvalJury.effectiveJudgments(verdict)
                    .get("second")));
    }

    @Test
    void testJudgeErrorDoesNotHideFailure() {
        Verdict verdict = AiAgentEvalJury.vote(
            List.of(named("first", Judgment.fail("wrong")), named("second", Judgment.error("unparseable reply"))));

        assertEquals(AiAgentEvalResultConclusion.INCONCLUSIVE, AiAgentEvalJury.toConclusion(verdict));
    }

    @Test
    void testThrowingJudgeIsRecordedAsError() {
        Judge throwingJudge = Judges.named(
            () -> {
                throw new IllegalStateException("boom");
            },
            "throwing");

        Verdict verdict = AiAgentEvalJury.vote(List.of(named("first", Judgment.pass("ok")), throwingJudge));

        Judgment judgment = AiAgentEvalJury.effectiveJudgments(verdict)
            .get("throwing");

        assertEquals(AiAgentJudgeVerdictStatus.ERROR, AiAgentEvalJury.toStatus(judgment));
        assertEquals("judge_failed", AiAgentEvalJury.toReasonCode(judgment));
        assertNull(AiAgentEvalJury.toScore(judgment));
        assertEquals(AiAgentEvalResultConclusion.INCONCLUSIVE, AiAgentEvalJury.toConclusion(verdict));
    }

    @Test
    void testAbstainingJudgeIsLeftOutOfTheDecision() {
        Verdict verdict = AiAgentEvalJury.vote(
            List.of(named("first", Judgment.pass("ok")), named("second", Judgment.abstain("no opinion"))));

        AiAgentEvalResultConclusion conclusion = AiAgentEvalJury.toConclusion(verdict);

        assertEquals(AiAgentEvalResultConclusion.PASS, conclusion);
        assertEquals(1.0, AiAgentEvalJury.toScore(verdict, conclusion));
        assertEquals(
            AiAgentJudgeVerdictStatus.ABSTAIN,
            AiAgentEvalJury.toStatus(
                AiAgentEvalJury.effectiveJudgments(verdict)
                    .get("second")));
    }

    @Test
    void testUndeclaredNotApplicableIsRecordedAsError() {
        Verdict verdict = AiAgentEvalJury.vote(
            List.of(named("first", Judgment.fail("wrong")), named("toolUsage", Judgment.notApplicable("no tools"))));

        Judgment judgment = AiAgentEvalJury.effectiveJudgments(verdict)
            .get("toolUsage");

        assertEquals(AiAgentJudgeVerdictStatus.ERROR, AiAgentEvalJury.toStatus(judgment));
        assertEquals("undeclared_not_applicable", AiAgentEvalJury.toReasonCode(judgment));
        assertEquals(AiAgentEvalResultConclusion.INCONCLUSIVE, AiAgentEvalJury.toConclusion(verdict));
    }

    @Test
    void testGradedScoreIsPreserved() {
        Judgment graded = Judgment.builder()
            .fail()
            .score(0.75)
            .reasoning("similarity below threshold")
            .build();

        Verdict verdict = AiAgentEvalJury.vote(List.of(named("similarity", graded)));

        AiAgentEvalResultConclusion conclusion = AiAgentEvalJury.toConclusion(verdict);

        assertEquals(AiAgentEvalResultConclusion.FAIL, conclusion);
        assertEquals(0.75, AiAgentEvalJury.toScore(verdict, conclusion));
        assertEquals(
            0.75,
            AiAgentEvalJury.toScore(
                AiAgentEvalJury.effectiveJudgments(verdict)
                    .get("similarity")));
    }

    private static Judge named(String name, Judgment judgment) {
        return Judges.named(() -> judgment, name);
    }
}
