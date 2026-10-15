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

package com.bytechef.component.ai.llm.router.requesty.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Thibault Jaigu
 */
class RequestyChatModelTest {

    private final List<Message> messages = List.of(new SystemMessage("system"), new UserMessage("user"));

    @Test
    void testBuildRequestBodyOmitsTextResponseFormat() {
        RequestyChatModel requestyChatModel = RequestyChatModel.builder()
            .apiKey("token")
            .model("openai/gpt-4o-mini")
            .reasoning("low")
            .temperature(0.5)
            .jsonResponseFormat(false)
            .build();

        Map<String, Object> body = requestyChatModel.buildRequestBody(messages);

        assertFalse(body.containsKey("response_format"));
        assertEquals(
            Map.of(
                "model", "openai/gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "system", "content", "system"), Map.of("role", "user", "content", "user")),
                "reasoning_effort", "low",
                "temperature", 0.5),
            body);
    }

    @Test
    void testBuildRequestBodyWithJsonResponseFormat() {
        RequestyChatModel requestyChatModel = RequestyChatModel.builder()
            .apiKey("token")
            .model("openai/gpt-4o-mini")
            .jsonResponseFormat(true)
            .build();

        Map<String, Object> body = requestyChatModel.buildRequestBody(messages);

        assertEquals(Map.of("type", "json_object"), body.get("response_format"));
    }
}
