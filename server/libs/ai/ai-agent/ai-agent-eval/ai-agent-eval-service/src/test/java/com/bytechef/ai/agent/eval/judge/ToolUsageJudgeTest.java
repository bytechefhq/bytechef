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
class ToolUsageJudgeTest {

    private static final String TRANSCRIPT_WITH_ONE_SEARCH_ORDERS = """
        {"messages":[
            {"role":"user","content":"Where is my order?"},
            {"role":"assistant","content":"Let me check.",
             "toolCalls":[{"toolName":"searchOrders","input":{},"output":"Order found"}]}
        ]}
        """;

    private static final String TRANSCRIPT_WITH_TWO_SEARCH_ORDERS = """
        {"messages":[
            {"role":"user","content":"Where are my orders?"},
            {"role":"assistant","content":"Checking first.",
             "toolCalls":[{"toolName":"searchOrders","input":{},"output":"Order 1"}]},
            {"role":"assistant","content":"Checking second.",
             "toolCalls":[{"toolName":"searchOrders","input":{},"output":"Order 2"}]}
        ]}
        """;

    private static final String TRANSCRIPT_WITH_SEARCH_FIRST_THEN_FORMAT = """
        {"messages":[
            {"role":"user","content":"Search and format."},
            {"role":"assistant","content":"Searching.",
             "toolCalls":[{"toolName":"searchOrders","input":{},"output":"data"},
                          {"toolName":"formatResult","input":{},"output":"formatted"}]}
        ]}
        """;

    @Test
    void testToolUsedAnywhereAtLeastOncePass() {
        ToolUsageJudge judge = new ToolUsageJudge("searchOrders", "ANYWHERE", "AT_LEAST", 1);

        Judgment judgment = judge.judge(buildContext(TRANSCRIPT_WITH_ONE_SEARCH_ORDERS));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testToolNotUsedFail() {
        ToolUsageJudge judge = new ToolUsageJudge("searchOrders", "ANYWHERE", "AT_LEAST", 1);

        String transcriptWithoutTool = """
            {"messages":[
                {"role":"user","content":"Hello"},
                {"role":"assistant","content":"Hi there"}
            ]}
            """;

        Judgment judgment = judge.judge(buildContext(transcriptWithoutTool));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testToolUsedExactlyTwoPass() {
        ToolUsageJudge judge = new ToolUsageJudge("searchOrders", "ANYWHERE", "EXACTLY", 2);

        Judgment judgment = judge.judge(buildContext(TRANSCRIPT_WITH_TWO_SEARCH_ORDERS));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testToolUsedAtMostOneWhenUsedTwoFail() {
        ToolUsageJudge judge = new ToolUsageJudge("searchOrders", "ANYWHERE", "AT_MOST", 1);

        Judgment judgment = judge.judge(buildContext(TRANSCRIPT_WITH_TWO_SEARCH_ORDERS));

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    @Test
    void testToolUsedFirstPositionPass() {
        ToolUsageJudge judge = new ToolUsageJudge("searchOrders", "FIRST", "AT_LEAST", 1);

        Judgment judgment = judge.judge(buildContext(TRANSCRIPT_WITH_SEARCH_FIRST_THEN_FORMAT));

        assertEquals(JudgmentStatus.PASS, judgment.status());
    }

    @Test
    void testMissingTranscriptFail() {
        ToolUsageJudge judge = new ToolUsageJudge("searchOrders", "ANYWHERE", "AT_LEAST", 1);

        JudgmentContext context = JudgmentContext.builder()
            .goal("test")
            .agentOutput("some output")
            .build();

        Judgment judgment = judge.judge(context);

        assertEquals(JudgmentStatus.FAIL, judgment.status());
    }

    private JudgmentContext buildContext(String transcript) {
        return JudgmentContext.builder()
            .goal("test")
            .agentOutput("some output")
            .metadata("transcript", transcript)
            .build();
    }
}
