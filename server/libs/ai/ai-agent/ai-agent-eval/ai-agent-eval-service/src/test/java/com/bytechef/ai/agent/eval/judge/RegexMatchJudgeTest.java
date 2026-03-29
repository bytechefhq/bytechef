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
class RegexMatchJudgeTest {

    @Test
    void testMustMatchWhenPatternMatches() {
        RegexMatchJudge judge = new RegexMatchJudge("\\d{3}-\\d{4}", "MUST_MATCH");

        Judgment judgment = judge.judge(buildContext("Call 555-1234 now"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMustMatchWhenPatternDoesNotMatch() {
        RegexMatchJudge judge = new RegexMatchJudge("\\d{3}-\\d{4}", "MUST_MATCH");

        Judgment judgment = judge.judge(buildContext("No phone number here"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testMustNotMatchWhenPatternDoesNotMatch() {
        RegexMatchJudge judge = new RegexMatchJudge("\\d{3}-\\d{4}", "MUST_NOT_MATCH");

        Judgment judgment = judge.judge(buildContext("No phone number here"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMustNotMatchWhenPatternMatches() {
        RegexMatchJudge judge = new RegexMatchJudge("\\d{3}-\\d{4}", "MUST_NOT_MATCH");

        Judgment judgment = judge.judge(buildContext("Call 555-1234 now"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testInvalidRegexReturnsError() {
        RegexMatchJudge judge = new RegexMatchJudge("[invalid", "MUST_MATCH");

        Judgment judgment = judge.judge(buildContext("some output"));

        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    @Test
    void testEmailPatternMatch() {
        RegexMatchJudge judge = new RegexMatchJudge("[a-zA-Z0-9.]+@[a-zA-Z0-9.]+", "MUST_MATCH");

        Judgment judgment = judge.judge(buildContext("Contact us at support@example.com"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    private JudgmentContext buildContext(String agentOutput) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput(agentOutput)
            .build();
    }

}
