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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;

/**
 * @author Ivica Cardic
 */
class HumanToolResponsesTest {

    private static final ToolResponse HUMAN_ANSWER = new ToolResponse("call_2", "askUserQuestion", "Blue");
    private static final ToolResponse LOOK_UP_RESULT = new ToolResponse("call_1", "lookUp", "looked up");

    @Test
    void testHumanResponseTextSurvivesReorderedResponses() {
        ToolResponseMessage markedToolResponseMessage = HumanToolResponses.markHumanResponse(
            ToolResponseMessage.builder()
                .responses(List.of(LOOK_UP_RESULT, HUMAN_ANSWER))
                .build(),
            HUMAN_ANSWER.id());

        ToolResponseMessage reorderedToolResponseMessage = ToolResponseMessage.builder()
            .responses(List.of(HUMAN_ANSWER, LOOK_UP_RESULT))
            .metadata(markedToolResponseMessage.getMetadata())
            .build();

        assertThat(HumanToolResponses.getHumanResponseText(reorderedToolResponseMessage)).isEqualTo("Blue");
    }
}
