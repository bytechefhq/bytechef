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

package com.bytechef.component.ai.llm.router.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NanoGptChatRequestTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "minimax/minimax-m3.1-flash", "minimax/minimax-m3", "minimax/minimax-m2.7",
        "MiniMax-M1", "MiniMaxAI/MiniMax-M1-80k", "minimax/minimax-m3:thinking",
        "custom/private-model", "other/model"
    })
    void testSelectedModelAndMultimodalContentReachHttpUnchanged(String modelId) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
            .build();
        Parameters parameters = MockParametersFactory.create(Map.of(MODEL, modelId));
        Parameters connection = MockParametersFactory.create(Map.of(TOKEN, "offline-test-key"));

        server.expect(requestTo("https://nano-gpt.com/api/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer offline-test-key"))
            .andExpect(content().json("""
                {
                    "model": "%s",
                    "messages": [{
                        "role": "user",
                        "content": [
                            {"type": "text", "text": "Describe this image"},
                            {"type": "image_url", "image_url": {"url": "data:image/png;base64,AQID"}}
                        ]
                    }],
                    "response_format": {"type": "text"}
                }
                """.replace("%s", modelId), JsonCompareMode.STRICT))
            .andRespond(withSuccess("""
                {"choices": [{"message": {"content": "Test response"}}]}
                """, MediaType.APPLICATION_JSON));

        try (MockedStatic<ModelUtils> mockedUtils = mockStatic(ModelUtils.class)) {
            mockedUtils.when(ModelUtils::getRestClientBuilder)
                .thenReturn(builder);

            ChatModel chatModel = NanoGptChatAction.CHAT_MODEL.createChatModel(parameters, connection, false);
            UserMessage message = UserMessage.builder()
                .text("Describe this image")
                .media(List.of(new Media(MediaType.IMAGE_PNG, new ByteArrayResource(new byte[] {
                    1, 2, 3
                }))))
                .build();

            assertEquals("Test response", chatModel.call(new Prompt(message))
                .getResults()
                .getFirst()
                .getOutput()
                .getText());
        }

        server.verify();
    }
}
