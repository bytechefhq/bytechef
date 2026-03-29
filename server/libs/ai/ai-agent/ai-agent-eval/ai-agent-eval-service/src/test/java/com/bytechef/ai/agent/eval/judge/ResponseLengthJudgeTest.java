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

import org.junit.jupiter.api.Test;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;

/**
 * @author ByteChef
 */
class ResponseLengthJudgeTest {

    @Test
    void testWithinRange() {
        ResponseLengthJudge judge = new ResponseLengthJudge(100, 5);

        Judgment judgment = judge.judge(buildContext("Hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testTooShort() {
        ResponseLengthJudge judge = new ResponseLengthJudge(100, 20);

        Judgment judgment = judge.judge(buildContext("Hi"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testTooLong() {
        ResponseLengthJudge judge = new ResponseLengthJudge(5, null);

        Judgment judgment = judge.judge(buildContext("This is a long response"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testNullMinLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(100, null);

        Judgment judgment = judge.judge(buildContext("Hi"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testNullMaxLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(null, 2);

        Judgment judgment = judge.judge(buildContext("Hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testBothThresholdsNull() {
        ResponseLengthJudge judge = new ResponseLengthJudge(null, null);

        Judgment judgment = judge.judge(buildContext("Any output"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testEmptyOutputWithMinLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(null, 1);

        Judgment judgment = judge.judge(buildContext(""));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testExactMinLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(null, 5);

        Judgment judgment = judge.judge(buildContext("12345"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testExactMaxLength() {
        ResponseLengthJudge judge = new ResponseLengthJudge(5, null);

        Judgment judgment = judge.judge(buildContext("12345"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    private JudgmentContext buildContext(String agentOutput) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput(agentOutput)
            .build();
    }

}
