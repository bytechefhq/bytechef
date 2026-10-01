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

package com.bytechef.automation.ai.tool.aiagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class DeleteAiAgentChannelToolCallbackTest {

    private final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
    private final JsonMapper jsonMapper = new JsonMapper();

    @BeforeEach
    void beforeEach() {
        AiAgentElement element = new AiAgentElement(1L, AiAgentElement.KIND_CHAT_MEMORY);

        element.setId(3L);

        AiAgentChannel channel = new AiAgentChannel(1L, "schedule");

        channel.setId(5L);

        when(aiAgentFacade.getAgent(1L)).thenReturn(
            new AiAgentDTO(new AiAgent(1L), 9L, List.of(channel), List.of(element), false, 0, null));
    }

    @Test
    void testCallRejectsAChannelOfAnotherAgent() {
        JsonNode result = jsonMapper.readTree(callback().call("{\"agentId\": 1, \"channelId\": 1076}"));

        assertThat(result.get("error")
            .asString()).contains("1076");

        verify(aiAgentFacade, never()).deleteAgentChannel(anyLong());
    }

    @Test
    void testCallDeletesAChannelOfTheAgent() {
        callback().call("{\"agentId\": 1, \"channelId\": 5}");

        verify(aiAgentFacade).deleteAgentChannel(5L);
    }

    private DeleteAiAgentChannelToolCallback callback() {
        return new DeleteAiAgentChannelToolCallback(aiAgentFacade);
    }
}
