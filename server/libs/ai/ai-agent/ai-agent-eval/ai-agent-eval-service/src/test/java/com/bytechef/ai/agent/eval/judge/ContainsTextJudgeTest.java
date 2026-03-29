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
class ContainsTextJudgeTest {

    @Test
    void testMustContainWhenTextIsPresent() {
        ContainsTextJudge judge = new ContainsTextJudge("hello", "MUST_CONTAIN");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMustContainWhenTextIsAbsent() {
        ContainsTextJudge judge = new ContainsTextJudge("goodbye", "MUST_CONTAIN");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testMustNotContainWhenTextIsAbsent() {
        ContainsTextJudge judge = new ContainsTextJudge("goodbye", "MUST_NOT_CONTAIN");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMustNotContainWhenTextIsPresent() {
        ContainsTextJudge judge = new ContainsTextJudge("hello", "MUST_NOT_CONTAIN");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCaseInsensitiveMatch() {
        ContainsTextJudge judge = new ContainsTextJudge("HELLO", "MUST_CONTAIN");

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCaseInsensitiveMustNotContain() {
        ContainsTextJudge judge = new ContainsTextJudge("HELLO", "MUST_NOT_CONTAIN");

        Judgment judgment = judge.judge(buildContext("Hello World"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testEmptyOutput() {
        ContainsTextJudge judge = new ContainsTextJudge("hello", "MUST_CONTAIN");

        Judgment judgment = judge.judge(buildContext(""));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    private JudgmentContext buildContext(String agentOutput) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput(agentOutput)
            .build();
    }

}
