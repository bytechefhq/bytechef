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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.ee.platform.ai.agent.eval.constant.AiAgentJudgeType;
import io.github.markpollack.judge.Judge;
import io.github.markpollack.judge.JudgeMetadata;
import io.github.markpollack.judge.JudgeType;
import io.github.markpollack.judge.Judges;
import io.github.markpollack.judge.NamedJudge;
import io.github.markpollack.judge.completion.CompletionEvidence;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author ByteChef
 */
class AgentJudgeFactoryTest {

    private final AiAgentJudgeFactory agentJudgeFactory = new AiAgentJudgeFactory();

    @Test
    void testCreateLlmRuleJudge() {
        Map<String, Object> configuration = Map.of("rule", "The response must be polite");

        Judge judge = agentJudgeFactory.createJudge("politeness", AiAgentJudgeType.LLM_RULE, configuration, null,
            () -> buildEvidence(""));

        assertNotNull(judge);
        assertInstanceOf(NamedJudge.class, judge);

        JudgeMetadata metadata = Judges.tryMetadata(judge)
            .orElseThrow();

        assertEquals("politeness", metadata.name());
        assertEquals(JudgeType.LLM_POWERED, metadata.type());
    }

    @Test
    void testCreateContainsTextJudge() {
        Map<String, Object> configuration = Map.of("text", "hello", "mode", "MUST_CONTAIN");

        Judge judge = agentJudgeFactory.createJudge(
            "containsHello", AiAgentJudgeType.CONTAINS_TEXT, configuration, null, () -> buildEvidence("hello world"));

        assertNotNull(judge);

        JudgeMetadata metadata = Judges.tryMetadata(judge)
            .orElseThrow();

        assertEquals("containsHello", metadata.name());
        assertEquals(JudgeType.DETERMINISTIC, metadata.type());

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCreateContainsTextJudgeFails() {
        Map<String, Object> configuration = Map.of("text", "goodbye", "mode", "MUST_CONTAIN");

        Judge judge = agentJudgeFactory.createJudge(
            "containsGoodbye", AiAgentJudgeType.CONTAINS_TEXT, configuration, null, () -> buildEvidence("hello world"));

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCreateRegexMatchJudge() {
        Map<String, Object> configuration = Map.of("pattern", "\\d{3}-\\d{4}", "mode", "MUST_MATCH");

        Judge judge = agentJudgeFactory.createJudge("phonePattern", AiAgentJudgeType.REGEX_MATCH, configuration, null,
            () -> buildEvidence("Call 555-1234 for info"));

        assertNotNull(judge);

        JudgeMetadata metadata = Judges.tryMetadata(judge)
            .orElseThrow();

        assertEquals("phonePattern", metadata.name());
        assertEquals(JudgeType.DETERMINISTIC, metadata.type());

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCreateResponseLengthJudge() {
        Map<String, Object> configuration = Map.of("maxLength", 100, "minLength", 5);

        Judge judge = agentJudgeFactory.createJudge(
            "lengthCheck", AiAgentJudgeType.RESPONSE_LENGTH, configuration, null,
            () -> buildEvidence("This is a valid response"));

        assertNotNull(judge);

        JudgeMetadata metadata = Judges.tryMetadata(judge)
            .orElseThrow();

        assertEquals("lengthCheck", metadata.name());
        assertEquals(JudgeType.DETERMINISTIC, metadata.type());

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCreateResponseLengthJudgeTooShort() {
        Map<String, Object> configuration = Map.of("maxLength", 100, "minLength", 50);

        Judge judge = agentJudgeFactory.createJudge(
            "lengthCheck", AiAgentJudgeType.RESPONSE_LENGTH, configuration, null, () -> buildEvidence("Short"));

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testCreateJsonSchemaJudge() {
        Map<String, Object> configuration = Map.of(
            "schema", Map.of("required", List.of("name", "age")));

        Judge judge = agentJudgeFactory.createJudge("jsonCheck", AiAgentJudgeType.JSON_SCHEMA, configuration, null,
            () -> buildEvidence("{\"name\": \"John\", \"age\": 30}"));

        assertNotNull(judge);

        JudgeMetadata metadata = Judges.tryMetadata(judge)
            .orElseThrow();

        assertEquals("jsonCheck", metadata.name());
        assertEquals(JudgeType.DETERMINISTIC, metadata.type());

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testCreateSimilarityJudge() {
        Map<String, Object> configuration = Map.of(
            "expectedOutput", "hello world",
            "threshold", 0.8,
            "algorithm", "EDIT_DISTANCE");

        Judge judge = agentJudgeFactory.createJudge(
            "similarityCheck", AiAgentJudgeType.SIMILARITY, configuration, null, () -> buildEvidence("hello world"));

        assertNotNull(judge);

        JudgeMetadata metadata = Judges.tryMetadata(judge)
            .orElseThrow();

        assertEquals("similarityCheck", metadata.name());
        assertEquals(JudgeType.DETERMINISTIC, metadata.type());

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
