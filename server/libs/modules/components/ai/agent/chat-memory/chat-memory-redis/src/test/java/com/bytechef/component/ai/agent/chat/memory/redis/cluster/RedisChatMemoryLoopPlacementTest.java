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

package com.bytechef.component.ai.agent.chat.memory.redis.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.ai.agent.chat.memory.redis.util.RedisChatMemoryUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

/**
 * @author Ivica Cardic
 */
class RedisChatMemoryLoopPlacementTest {

    @Test
    void testRunsOutsideTheToolLoop() {
        Parameters parameters = mock(Parameters.class);

        try (MockedStatic<RedisChatMemoryUtils> utilsMockedStatic = mockStatic(RedisChatMemoryUtils.class)) {
            utilsMockedStatic.when(() -> RedisChatMemoryUtils.getChatMemoryRepository(any(Parameters.class)))
                .thenReturn(mock(ChatMemoryRepository.class));

            ChatMemoryFunction.Result result = RedisChatMemory.apply(parameters, parameters, parameters, Map.of());

            assertFalse(result.supportsToolMessagePersistence());
            assertNotEquals(
                ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER, result.advisor()
                    .getOrder());
        }
    }
}
