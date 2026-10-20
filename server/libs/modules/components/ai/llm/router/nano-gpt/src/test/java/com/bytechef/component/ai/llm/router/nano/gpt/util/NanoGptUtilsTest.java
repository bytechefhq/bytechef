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

package com.bytechef.component.ai.llm.router.nano.gpt.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NanoGptUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters parameters = MockParametersFactory.create(Map.of());

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any())).thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any())).thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(), any())).thenReturn(mockedExecutor);
        when(mockedExecutor.execute()).thenReturn(mockedResponse);
    }

    @Test
    void testChatModelsPreferFlashAndRemoveLegacyAliases() throws Exception {
        mockModels(List.of(
            model("other/first", 0), model("minimax/minimax-m2.7", 1), model("MiniMax-M1", 1),
            model("MiniMaxAI/MiniMax-M1-80k", 1), model("minimax/minimax-m3:thinking", 1),
            model("minimax/minimax-m3", 2), model("other/second", 3),
            model("minimax/minimax-m3.1-flash", 4)));

        assertEquals(List.of(
            option("other/first - $0/$0 per 1M tokens", "other/first", "Gateway description"),
            option("minimax/minimax-m3.1-flash - $4/$0 per 1M tokens", "minimax/minimax-m3.1-flash", "Gateway description"),
            option("minimax/minimax-m3 - $2/$0 per 1M tokens", "minimax/minimax-m3", "Gateway description"),
            option("other/second - $3/$0 per 1M tokens", "other/second", "Gateway description")),
            NanoGptUtils.getNanoGptChatModels().apply(parameters, parameters, Map.of(), "", mockedContext));
        verify(mockedExecutor).queryParameter("detailed", "true");
    }

    @Test
    void testChatModelsDoNotInventUnavailableRoutes() throws Exception {
        mockModels(List.of(model("minimax/minimax-m2.7", 0), model("minimax/minimax-m3", 1)));

        assertEquals(List.of(
            option("minimax/minimax-m3 - $1/$0 per 1M tokens", "minimax/minimax-m3", "Gateway description")),
            NanoGptUtils.getNanoGptChatModels().apply(parameters, parameters, Map.of(), "", mockedContext));
    }

    @Test
    void testNonTextModelsRemainUnfiltered() throws Exception {
        mockModels(List.of(Map.of("id", "minimax/nontext", "name", "MiniMax nontext", "description", "Description")));

        List<Option<String>> expected = List.of(option("MiniMax nontext", "minimax/nontext", "Description"));

        assertEquals(expected, NanoGptUtils.getNanoGptImageModels()
            .apply(parameters, parameters, Map.of(), "", mockedContext));
        assertEquals(expected, NanoGptUtils.getNanoGptSpeechModels()
            .apply(parameters, parameters, Map.of(), "", mockedContext));
        assertEquals(expected, NanoGptUtils.getNanoGptTranscriptionModels()
            .apply(parameters, parameters, Map.of(), "", mockedContext));
        assertEquals(expected, NanoGptUtils.getNanoGptEmbeddingModels()
            .apply(parameters, parameters, Map.of(), "", mockedContext));
    }

    private static Map<String, Object> model(String id, double price) {
        return Map.of("id", id, "name", id, "description", "Gateway description", "pricing", Map.of("prompt", price));
    }

    private void mockModels(List<Map<String, Object>> models) {
        ObjectMapper objectMapper = new ObjectMapper();

        when(mockedResponse.getBody(any(TypeReference.class))).thenAnswer(invocation -> {
            TypeReference<?> typeReference = invocation.getArgument(0);

            return objectMapper.convertValue(Map.of("data", models), objectMapper.constructType(typeReference.getType()));
        });
    }
}
