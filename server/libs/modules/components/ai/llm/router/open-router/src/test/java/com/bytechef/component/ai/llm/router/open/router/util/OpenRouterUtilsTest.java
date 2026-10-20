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

package com.bytechef.component.ai.llm.router.open.router.util;

import static com.bytechef.component.ai.llm.router.constant.RouterConstants.SUPPORTED_PARAMETERS;
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

class OpenRouterUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(SUPPORTED_PARAMETERS, List.of("tools", "reasoning")));

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any())).thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any())).thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(), any())).thenReturn(mockedExecutor);
        when(mockedExecutor.execute()).thenReturn(mockedResponse);
    }

    @Test
    void testTextModelsPreferFlashAndPreserveGatewayMetadata() throws Exception {
        mockModels(List.of(
            model("other/first", "0"), model("minimax/minimax-m2.7", "1"),
            model("minimax/minimax-m3", "2"), model("other/second", "3"),
            model("minimax/minimax-m3.1-flash:free", "4")));

        List<? extends Option<String>> options = OpenRouterUtils.getOpenRouterModels("text")
            .apply(parameters, parameters, Map.of(), "", mockedContext);

        assertEquals(List.of(
            option("other/first - $0", "other/first", "Gateway description"),
            option("minimax/minimax-m3.1-flash:free - $4", "minimax/minimax-m3.1-flash:free", "Gateway description"),
            option("minimax/minimax-m3 - $2", "minimax/minimax-m3", "Gateway description"),
            option("other/second - $3", "other/second", "Gateway description")), options);
        verify(mockedExecutor).queryParameter("output_modalities", "text");
        verify(mockedExecutor).queryParameter("supported_parameters", "tools,reasoning");
    }

    @Test
    void testTextModelsDoNotInventUnavailableFlashRoute() throws Exception {
        mockModels(List.of(model("minimax/minimax-m2.7", "0"), model("minimax/minimax-m3", "1")));

        assertEquals(List.of(option("minimax/minimax-m3 - $1", "minimax/minimax-m3", "Gateway description")),
            OpenRouterUtils.getOpenRouterModels("text")
                .apply(parameters, parameters, Map.of(), "", mockedContext));
    }

    @Test
    void testNonTextAndEmbeddingModelsRemainUnfiltered() throws Exception {
        mockModels(List.of(model("minimax/image-01", "1"), model("other/model", "0")));

        List<Option<String>> expected = List.of(
            option("other/model - $0", "other/model", "Gateway description"),
            option("minimax/image-01 - $1", "minimax/image-01", "Gateway description"));

        for (String outputType : List.of("image", "audio")) {
            assertEquals(expected, OpenRouterUtils.getOpenRouterModels(outputType)
                .apply(parameters, parameters, Map.of(), "", mockedContext));
        }

        assertEquals(expected, OpenRouterUtils.getOpenRouterEmbeddingModels()
            .apply(parameters, parameters, Map.of(), "", mockedContext));
    }

    private static Map<String, Object> model(String id, String price) {
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
