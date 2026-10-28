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
 * @author Ivica Cardic
 */
class StringEqualsJudgeTest {

    @Test
    void testCaseSensitiveExactMatchPass() {
        StringEqualsJudge judge = new StringEqualsJudge(() -> buildEvidence("hello world"), "hello world", true);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCaseSensitiveExactMatchFail() {
        StringEqualsJudge judge = new StringEqualsJudge(() -> buildEvidence("Hello World"), "hello world", true);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCaseInsensitiveMatchPass() {
        StringEqualsJudge judge = new StringEqualsJudge(() -> buildEvidence("Hello World"), "hello world", false);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testDifferentTextFail() {
        StringEqualsJudge judge = new StringEqualsJudge(() -> buildEvidence("goodbye world"), "hello world", true);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testEmptyOutputMatchesEmptyExpected() {
        StringEqualsJudge judge = new StringEqualsJudge(() -> buildEvidence(""), "", true);

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
