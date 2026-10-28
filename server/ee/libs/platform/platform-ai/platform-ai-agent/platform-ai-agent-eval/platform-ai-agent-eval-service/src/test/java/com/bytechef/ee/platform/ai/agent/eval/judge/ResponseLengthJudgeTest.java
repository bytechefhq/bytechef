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

import io.github.markpollack.judge.completion.CompletionEvidence;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import org.junit.jupiter.api.Test;

/**
 * @author ByteChef
 */
class ResponseLengthJudgeTest {

    @Test
    void testWithinRange() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("Hello world"), 100, 5);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testTooShort() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("Hi"), 100, 20);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testTooLong() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("This is a long response"), 5, null);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testNullMinLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("Hi"), 100, null);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testNullMaxLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("Hello world"), null, 2);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testBothThresholdsNull() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("Any output"), null, null);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testEmptyOutputWithMinLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence(""), null, 1);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testExactMinLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("12345"), null, 5);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testExactMaxLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(() -> buildEvidence("12345"), 5, null);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    private CompletionEvidence buildEvidence(String agentOutput) {
        return CompletionEvidence.builder()
            .request("test")
            .response(agentOutput)
            .build();
    }

}
