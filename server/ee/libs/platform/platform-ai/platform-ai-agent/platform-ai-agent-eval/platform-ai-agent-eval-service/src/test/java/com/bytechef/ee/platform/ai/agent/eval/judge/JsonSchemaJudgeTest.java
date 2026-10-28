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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.markpollack.judge.completion.CompletionEvidence;
import io.github.markpollack.judge.judgment.Judgment;
import io.github.markpollack.judge.judgment.JudgmentStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author ByteChef
 */
class JsonSchemaJudgeTest {

    @Test
    void testValidJsonWithAllRequiredFields() {
        Map<String, Object> schema = Map.of("required", List.of("name", "age"));

        JsonSchemaJudge judge = new JsonSchemaJudge(() -> buildEvidence("{\"name\": \"John\", \"age\": 30}"), schema);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testValidJsonMissingRequiredField() {
        Map<String, Object> schema = Map.of("required", List.of("name", "age", "email"));

        JsonSchemaJudge judge = new JsonSchemaJudge(() -> buildEvidence("{\"name\": \"John\", \"age\": 30}"), schema);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());

        assertTrue(judgment.reasoning()
            .contains("email"));
    }

    @Test
    void testInvalidJson() {
        Map<String, Object> schema = Map.of("required", List.of("name"));

        JsonSchemaJudge judge = new JsonSchemaJudge(() -> buildEvidence("not valid json"), schema);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.FAIL, judgment.status());

        assertTrue(judgment.reasoning()
            .contains("not valid JSON"));
    }

    @Test
    void testNoRequiredFields() {
        Map<String, Object> schema = Map.of("type", "object");

        JsonSchemaJudge judge = new JsonSchemaJudge(() -> buildEvidence("{\"anything\": \"goes\"}"), schema);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testValidJsonWithExtraFields() {
        Map<String, Object> schema = Map.of("required", List.of("name"));

        JsonSchemaJudge judge =
            new JsonSchemaJudge(() -> buildEvidence("{\"name\": \"John\", \"age\": 30, \"extra\": true}"), schema);

        Judgment judgment = judge.judge();

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testEmptyJsonObject() {
        Map<String, Object> schema = Map.of("required", List.of("name"));

        JsonSchemaJudge judge = new JsonSchemaJudge(() -> buildEvidence("{}"), schema);

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
