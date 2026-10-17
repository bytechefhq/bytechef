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

package com.bytechef.ai.copilot.tool.ask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 *
 * @author Ivica Cardic
 */
class SubAgentQuestionFormatterTest {

    @Test
    void testTwoOptionQuestionRendersNumberedOptionsQuestionTextAndReInvokeSentence() {
        String envelope =
            """
                {
                    "kind": "ask-user-question",
                    "questions": [
                        {
                            "question": "Which project should the workflow live in?",
                            "header": "Project",
                            "multiSelect": false,
                            "options": [
                                {"label": "CRM Project", "description": "The primary CRM automation project"},
                                {"label": "Marketing Project", "description": "Campaign automation workflows"}
                            ]
                        }
                    ],
                    "awaitingAnswer": true
                }""";

        String formatted = SubAgentQuestionFormatter.format("configureMcpServer", envelope);

        assertThat(formatted).contains("configureMcpServer agent needs a decision before continuing");
        assertThat(formatted).contains("Which project should the workflow live in?");
        assertThat(formatted).contains("1. CRM Project — The primary CRM automation project");
        assertThat(formatted).contains("2. Marketing Project — Campaign automation workflows");
        assertThat(formatted).contains(
            "Present these options to the user. Then call configureMcpServer again, restating the original request "
                + "together with the chosen answer — this agent does not carry anything over from the call that "
                + "asked, so an answer on its own is not enough to act on.");
    }

    @Test
    void testMultipleQuestionsAreEachRenderedWithTheirOwnNumberedOptions() {
        String envelope =
            """
                {
                    "kind": "ask-user-question",
                    "questions": [
                        {
                            "question": "Which project?",
                            "header": "Project",
                            "options": [
                                {"label": "CRM", "description": "CRM project"},
                                {"label": "Marketing", "description": "Marketing project"}
                            ]
                        },
                        {
                            "question": "Which environment?",
                            "header": "Env",
                            "options": [
                                {"label": "Development", "description": "Safe for testing"},
                                {"label": "Production", "description": "Live traffic"}
                            ]
                        }
                    ],
                    "awaitingAnswer": true
                }""";

        String formatted = SubAgentQuestionFormatter.format("configureMcpServer", envelope);

        assertThat(formatted).contains("Which project?");
        assertThat(formatted).contains("Which environment?");
        assertThat(formatted).contains("1. CRM — CRM project");
        assertThat(formatted).contains("2. Marketing — Marketing project");
        assertThat(formatted).contains("1. Development — Safe for testing");
        assertThat(formatted).contains("2. Production — Live traffic");
    }

    @Test
    void testOptionWithNoDescriptionRendersLabelOnly() {
        String envelope =
            """
                {
                    "kind": "ask-user-question",
                    "questions": [
                        {
                            "question": "Pick one",
                            "options": [
                                {"label": "Option A"},
                                {"label": "Option B", "description": ""}
                            ]
                        }
                    ]
                }""";

        String formatted = SubAgentQuestionFormatter.format("buildWorkflow", envelope);

        assertThat(formatted).contains("1. Option A\n");
        assertThat(formatted).contains("2. Option B\n");
        assertThat(formatted).doesNotContain("Option A —");
        assertThat(formatted).doesNotContain("Option B —");
    }

    @Test
    void testQuestionWithNoOptionsDoesNotThrow() {
        String envelope =
            """
                {
                    "kind": "ask-user-question",
                    "questions": [
                        {
                            "question": "Pick one"
                        }
                    ]
                }""";

        String formatted = SubAgentQuestionFormatter.format("buildWorkflow", envelope);

        assertThat(formatted).contains("Pick one");
        assertThat(formatted).contains("(no options provided)");
    }

    @Test
    void testEmptyQuestionsArrayFallsBackToRawEnvelope() {
        String envelope = "{\"kind\":\"ask-user-question\",\"questions\":[]}";

        String formatted = SubAgentQuestionFormatter.format("buildWorkflow", envelope);

        assertThat(formatted).isEqualTo(envelope);
    }

    @Test
    void testMissingQuestionsFieldFallsBackToRawEnvelope() {
        String envelope = "{\"kind\":\"ask-user-question\"}";

        String formatted = SubAgentQuestionFormatter.format("buildWorkflow", envelope);

        assertThat(formatted).isEqualTo(envelope);
    }

    @Test
    void testUnparseableEnvelopeFallsBackToRawInput() {
        String notJson = "this is not json";

        String formatted = SubAgentQuestionFormatter.format("buildWorkflow", notJson);

        assertThat(formatted).isEqualTo(notJson);
    }
}
