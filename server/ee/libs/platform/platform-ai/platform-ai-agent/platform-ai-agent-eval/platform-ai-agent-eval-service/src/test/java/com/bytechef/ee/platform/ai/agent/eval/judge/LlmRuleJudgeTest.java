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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.markpollack.judge.completion.CompletionEvidence;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author ByteChef
 */
class LlmRuleJudgeTest {

    private static final String RULE = "The agent must respond politely";

    private final LlmRuleJudge judge =
        new LlmRuleJudge(() -> buildEvidence("", ""), "PolitenessJudge", null, RULE, false);

    @Test
    void testBuildPromptContainsRule() {
        CompletionEvidence evidence = buildEvidence("Hello!", "User: Hi\nAgent: Hello!");

        String prompt = judge.buildPrompt(evidence);

        assertTrue(prompt.contains(RULE));
    }

    @Test
    void testBuildPromptContainsTranscript() {
        String transcript = "User: Hi\nAgent: Hello!";

        CompletionEvidence evidence = buildEvidence("Hello!", transcript);

        String prompt = judge.buildPrompt(evidence);

        assertTrue(prompt.contains(transcript));
    }

    @Test
    void testBuildPromptContainsAgentOutput() {
        String agentOutput = "Hello, how can I help you?";

        CompletionEvidence evidence = buildEvidence(agentOutput, "User: Hi\nAgent: Hello!");

        String prompt = judge.buildPrompt(evidence);

        assertTrue(prompt.contains(agentOutput));
    }

    @Test
    void testBuildPromptWithNoTranscript() {
        CompletionEvidence evidence = CompletionEvidence.builder()
            .request("test")
            .response("output")
            .build();

        String prompt = judge.buildPrompt(evidence);

        assertTrue(prompt.contains(RULE));
        assertTrue(prompt.contains("output"));
    }

    @Test
    void testParseResponsePassedTrue() {
        String response = "{\"passed\": true, \"explanation\": \"The agent was polite\"}";

        CompletionEvidence evidence = buildEvidence("Hello!", "transcript");

        Judgment judgment = judge.parseResponse(response, evidence);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals("The agent was polite", judgment.reasoning());
    }

    @Test
    void testParseResponsePassedFalse() {
        String response = "{\"passed\": false, \"explanation\": \"The agent was rude\"}";

        CompletionEvidence evidence = buildEvidence("Go away!", "transcript");

        Judgment judgment = judge.parseResponse(response, evidence);

        assertEquals(JudgmentStatus.FAIL, judgment.status());
        assertEquals("The agent was rude", judgment.reasoning());
    }

    @Test
    void testParseResponseMalformedJson() {
        String response = "This is not JSON at all";

        CompletionEvidence evidence = buildEvidence("output", "transcript");

        Judgment judgment = judge.parseResponse(response, evidence);

        assertEquals(JudgmentStatus.ERROR, judgment.status());
        assertNotNull(judgment.reasoning());
    }

    @Test
    void testParseResponseWithExtraFields() {
        String response =
            "{\"passed\": true, \"explanation\": \"Looks good\", \"confidence\": 0.95, \"details\": \"extra\"}";

        CompletionEvidence evidence = buildEvidence("output", "transcript");

        Judgment judgment = judge.parseResponse(response, evidence);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals("Looks good", judgment.reasoning());
    }

    @Test
    void testParseResponseWithWhitespace() {
        String response = "  \n  {\"passed\": true, \"explanation\": \"ok\"}  \n  ";

        CompletionEvidence evidence = buildEvidence("output", "transcript");

        Judgment judgment = judge.parseResponse(response, evidence);

        assertEquals(JudgmentStatus.PASS, judgment.status());
        assertEquals("ok", judgment.reasoning());
    }

    @Test
    void testParseResponseEmptyString() {
        CompletionEvidence evidence = buildEvidence("output", "transcript");

        Judgment judgment = judge.parseResponse("", evidence);

        assertEquals(JudgmentStatus.ERROR, judgment.status());
    }

    private CompletionEvidence buildEvidence(String agentOutput, String transcript) {
        return CompletionEvidence.builder()
            .request("test")
            .response(agentOutput)
            .metadata(Map.of("transcript", transcript))
            .build();
    }

}
