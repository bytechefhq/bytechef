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

package com.bytechef.component.ai.llm.router.open.router.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY;
import static com.bytechef.component.ai.llm.router.constant.RouterConstants.LOGPROBS;
import static com.bytechef.component.ai.llm.router.constant.RouterConstants.MAX_COMPLETION_TOKENS;
import static com.bytechef.component.ai.llm.router.constant.RouterConstants.TOP_LOGPROBS;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.router.open.router.model.OpenRouterChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class OpenRouterChatActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(TOKEN, "token"), Map.entry(MODEL, "model"),
            Map.entry(FREQUENCY_PENALTY, 1.0), Map.entry(LOGIT_BIAS, Map.of()),
            Map.entry(LOGPROBS, false), Map.entry(MAX_COMPLETION_TOKENS, 1),
            Map.entry(MAX_TOKENS, 1), Map.entry(PRESENCE_PENALTY, 0.0),
            Map.entry(REASONING, "reasoning"), Map.entry(SEED, 4),
            Map.entry(STOP, List.of()), Map.entry(TEMPERATURE, 0.0),
            Map.entry(TOP_K, 0.0), Map.entry(TOP_LOGPROBS, 1), Map.entry(TOP_P, 0.0),
            Map.entry(VERBOSITY, "verbosity"), Map.entry(USER, "user")));

    @Test
    void testPerform() {
        OpenRouterChatModel openRouterChatModel = (OpenRouterChatModel) OpenRouterChatAction.CHAT_MODEL.createChatModel(
            mockedParameters, mockedParameters, false);

        assertNotNull(openRouterChatModel);

        assertEquals("model", openRouterChatModel.getModel());
        assertEquals(1.0, openRouterChatModel.getFrequencyPenalty());
        assertEquals(Map.of(), openRouterChatModel.getLogitBias());
        assertEquals(false, openRouterChatModel.getLogprobs());
        assertEquals(1, openRouterChatModel.getMaxCompletionTokens());
        assertEquals(1, openRouterChatModel.getMaxTokens());
        assertEquals(0.0, openRouterChatModel.getPresencePenalty());
        assertEquals("reasoning", openRouterChatModel.getReasoning());
        assertEquals(4, openRouterChatModel.getSeed());
        assertEquals(List.of(), openRouterChatModel.getStop());
        assertEquals(0.0, openRouterChatModel.getTemperature());
        assertEquals(0.0, openRouterChatModel.getTopK());
        assertEquals(1, openRouterChatModel.getTopLogprobs());
        assertEquals(0.0, openRouterChatModel.getTopP());
        assertEquals("verbosity", openRouterChatModel.getVerbosity());
        assertEquals("user", openRouterChatModel.getUser());
    }
}
