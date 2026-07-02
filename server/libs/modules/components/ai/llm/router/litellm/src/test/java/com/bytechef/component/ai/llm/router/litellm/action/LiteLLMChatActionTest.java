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

package com.bytechef.component.ai.llm.router.litellm.action;

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
import static com.bytechef.component.ai.llm.router.litellm.constant.LiteLLMConstants.BASE_URL;
import static com.bytechef.component.ai.llm.router.litellm.constant.LiteLLMConstants.DEFAULT_BASE_URL;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.router.litellm.model.LiteLLMChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Aarish Yadav
 */
class LiteLLMChatActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(TOKEN, "token"), Map.entry(BASE_URL, DEFAULT_BASE_URL),
            Map.entry(MODEL, "gpt-4o"),
            Map.entry(FREQUENCY_PENALTY, 1.0), Map.entry(LOGIT_BIAS, Map.of()),
            Map.entry(LOGPROBS, false), Map.entry(MAX_COMPLETION_TOKENS, 1),
            Map.entry(MAX_TOKENS, 1), Map.entry(PRESENCE_PENALTY, 0.0),
            Map.entry(REASONING, "medium"), Map.entry(SEED, 4),
            Map.entry(STOP, List.of()), Map.entry(TEMPERATURE, 0.0),
            Map.entry(TOP_K, 0.0), Map.entry(TOP_LOGPROBS, 1), Map.entry(TOP_P, 0.0),
            Map.entry(VERBOSITY, "verbosity"), Map.entry(USER, "user")));

    @Test
    void testPerform() {
        LiteLLMChatModel liteLLMChatModel = (LiteLLMChatModel) LiteLLMChatAction.CHAT_MODEL.createChatModel(
            mockedParameters, mockedParameters, false);

        assertNotNull(liteLLMChatModel);

        assertEquals("gpt-4o", liteLLMChatModel.getModel());
        assertEquals(1.0, liteLLMChatModel.getFrequencyPenalty());
        assertEquals(Map.of(), liteLLMChatModel.getLogitBias());
        assertEquals(false, liteLLMChatModel.getLogprobs());
        assertEquals(1, liteLLMChatModel.getMaxCompletionTokens());
        assertEquals(1, liteLLMChatModel.getMaxTokens());
        assertEquals(0.0, liteLLMChatModel.getPresencePenalty());
        assertEquals("medium", liteLLMChatModel.getReasoning());
        assertEquals(4, liteLLMChatModel.getSeed());
        assertEquals(List.of(), liteLLMChatModel.getStop());
        assertEquals(0.0, liteLLMChatModel.getTemperature());
        assertEquals(0.0, liteLLMChatModel.getTopK());
        assertEquals(1, liteLLMChatModel.getTopLogprobs());
        assertEquals(0.0, liteLLMChatModel.getTopP());
        assertEquals("verbosity", liteLLMChatModel.getVerbosity());
        assertEquals("user", liteLLMChatModel.getUser());
    }
}
