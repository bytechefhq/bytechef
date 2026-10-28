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
class ContainsTextJudgeTest {

    @Test
    void testMustContainWhenTextIsPresent() {
        ContainsTextJudge judge = new ContainsTextJudge(() -> buildEvidence("hello world"), "hello", "MUST_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMustContainWhenTextIsAbsent() {
        ContainsTextJudge judge = new ContainsTextJudge(() -> buildEvidence("hello world"), "goodbye", "MUST_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testMustNotContainWhenTextIsAbsent() {
        ContainsTextJudge judge =
            new ContainsTextJudge(() -> buildEvidence("hello world"), "goodbye", "MUST_NOT_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMustNotContainWhenTextIsPresent() {
        ContainsTextJudge judge =
            new ContainsTextJudge(() -> buildEvidence("hello world"), "hello", "MUST_NOT_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCaseInsensitiveMatch() {
        ContainsTextJudge judge = new ContainsTextJudge(() -> buildEvidence("hello world"), "HELLO", "MUST_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCaseInsensitiveMustNotContain() {
        ContainsTextJudge judge =
            new ContainsTextJudge(() -> buildEvidence("Hello World"), "HELLO", "MUST_NOT_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testEmptyOutput() {
        ContainsTextJudge judge = new ContainsTextJudge(() -> buildEvidence(""), "hello", "MUST_CONTAIN");

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    private CompletionEvidence buildEvidence(String agentOutput) {
        return CompletionEvidence.builder()
            .request("test")
            .response(agentOutput)
            .build();
    }

}
