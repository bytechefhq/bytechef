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
 * @author Ivica Cardic
 */
class StringEqualsJudgeTest {

    @Test
    void testCaseSensitiveExactMatchPass() {
        StringEqualsJudge judge = new StringEqualsJudge("hello world", true);

        Judgment judgment = judge.judge(buildContext("hello world"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCaseSensitiveExactMatchFail() {
        StringEqualsJudge judge = new StringEqualsJudge("hello world", true);

        Judgment judgment = judge.judge(buildContext("Hello World"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCaseInsensitiveMatchPass() {
        StringEqualsJudge judge = new StringEqualsJudge("hello world", false);

        Judgment judgment = judge.judge(buildContext("Hello World"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testDifferentTextFail() {
        StringEqualsJudge judge = new StringEqualsJudge("hello world", true);

        Judgment judgment = judge.judge(buildContext("goodbye world"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testEmptyOutputMatchesEmptyExpected() {
        StringEqualsJudge judge = new StringEqualsJudge("", true);

        Judgment judgment = judge.judge(buildContext(""));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    private JudgmentContext buildContext(String agentOutput) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput(agentOutput)
            .build();
    }
}
