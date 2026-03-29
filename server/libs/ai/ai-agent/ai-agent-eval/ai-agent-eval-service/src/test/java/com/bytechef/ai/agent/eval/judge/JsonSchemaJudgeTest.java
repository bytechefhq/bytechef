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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.judge.context.JudgmentContext;
import org.springaicommunity.judge.result.Judgment;
import org.springaicommunity.judge.result.JudgmentStatus;

/**
 * @author ByteChef
 */
class JsonSchemaJudgeTest {

    @Test
    void testValidJsonWithAllRequiredFields() {
        Map<String, Object> schema = Map.of("required", List.of("name", "age"));

        JsonSchemaJudge judge = new JsonSchemaJudge(schema);

        Judgment judgment = judge.judge(buildContext("{\"name\": \"John\", \"age\": 30}"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testValidJsonMissingRequiredField() {
        Map<String, Object> schema = Map.of("required", List.of("name", "age", "email"));

        JsonSchemaJudge judge = new JsonSchemaJudge(schema);

        Judgment judgment = judge.judge(buildContext("{\"name\": \"John\", \"age\": 30}"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());

        assertTrue(judgment.reasoning()
            .contains("email"));
    }

    @Test
    void testInvalidJson() {
        Map<String, Object> schema = Map.of("required", List.of("name"));

        JsonSchemaJudge judge = new JsonSchemaJudge(schema);

        Judgment judgment = judge.judge(buildContext("not valid json"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());

        assertTrue(judgment.reasoning()
            .contains("not valid JSON"));
    }

    @Test
    void testNoRequiredFields() {
        Map<String, Object> schema = Map.of("type", "object");

        JsonSchemaJudge judge = new JsonSchemaJudge(schema);

        Judgment judgment = judge.judge(buildContext("{\"anything\": \"goes\"}"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testValidJsonWithExtraFields() {
        Map<String, Object> schema = Map.of("required", List.of("name"));

        JsonSchemaJudge judge = new JsonSchemaJudge(schema);

        Judgment judgment = judge.judge(
            buildContext("{\"name\": \"John\", \"age\": 30, \"extra\": true}"));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testEmptyJsonObject() {
        Map<String, Object> schema = Map.of("required", List.of("name"));

        JsonSchemaJudge judge = new JsonSchemaJudge(schema);

        Judgment judgment = judge.judge(buildContext("{}"));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    private JudgmentContext buildContext(String agentOutput) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput(agentOutput)
            .build();
    }

}
