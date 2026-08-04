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

package com.bytechef.component.ai.llm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.retry.NonTransientAiException;

/**
 * Pins the provider-error extraction in {@link ModelUtils#getChatResponse}: provider APIs disagree on the error body
 * shape, and a shape the extraction does not recognize must fall back to the raw exception message instead of dying
 * with a JSON-path lookup error that masks the real failure.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ModelUtilsTest {

    private final CallResponseSpec callResponseSpec = mock(CallResponseSpec.class);
    private final Context context = mock(Context.class);
    private final Parameters parameters = mock(Parameters.class);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        Context.Json json = mock(Context.Json.class);

        when(json.read(anyString(), any(TypeReference.class)))
            .thenAnswer(invocation -> JsonUtils.read(invocation.getArgument(0, String.class)));

        when(context.json(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.Json, ?> contextFunction = invocation.getArgument(0);

            return contextFunction.apply(json);
        });
    }

    @Test
    void testGetChatResponseExtractsOpenAiStyleErrorMessage() {
        when(callResponseSpec.chatResponse()).thenThrow(
            new NonTransientAiException("HTTP 429 - {\"error\": {\"message\": \"Rate limit reached\"}}"));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ModelUtils.getChatResponse(callResponseSpec, parameters, false, context));

        assertEquals("Rate limit reached", providerException.getMessage());
    }

    @Test
    void testGetChatResponseExtractsMistralStyleErrorMessage() {
        when(callResponseSpec.chatResponse()).thenThrow(
            new NonTransientAiException(
                "HTTP 429 - {\"object\": \"error\", \"message\": \"Requests rate limit exceeded\", \"type\": " +
                    "\"too_many_requests\"}"));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ModelUtils.getChatResponse(callResponseSpec, parameters, false, context));

        assertEquals("Requests rate limit exceeded", providerException.getMessage());
    }

    @Test
    void testGetChatResponseFallsBackToRawMessageOnUnrecognizedErrorShape() {
        String message = "HTTP 422 - {\"detail\": [{\"msg\": \"Field required\"}]}";

        when(callResponseSpec.chatResponse()).thenThrow(new NonTransientAiException(message));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ModelUtils.getChatResponse(callResponseSpec, parameters, false, context));

        assertEquals(message, providerException.getMessage());
    }

    @Test
    void testGetChatResponseFallsBackToRawMessageOnUnparseableBody() {
        String message = "Something failed around {this is not json} entirely";

        when(callResponseSpec.chatResponse()).thenThrow(new NonTransientAiException(message));

        ProviderException providerException = assertThrows(
            ProviderException.class,
            () -> ModelUtils.getChatResponse(callResponseSpec, parameters, false, context));

        assertEquals(message, providerException.getMessage());
    }
}
