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

package com.bytechef.component.ai.agent.guardrails.nsfw.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_SYSTEM_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

@ExtendWith(ObjectMapperSetupExtension.class)
class NsfwTest {

    @Test
    void testFlaggedAboveThresholdProducesViolation() {
        ChatClient client = mockClient(new LlmClassifier.Response(0.9, true));

        Optional<Violation> violation = Nsfw.classifyWith(
            client,
            "nsfw user prompt",
            0.7,
            DEFAULT_SYSTEM_MESSAGE,
            "explicit content");

        assertThat(violation).isPresent();
        assertThat(violation.get()
            .guardrail()).isEqualTo("nsfw");
        assertThat(((Violation.ClassifiedViolation) violation.get()).confidenceScore()).isEqualTo(0.9);
    }

    @Test
    void testBelowThresholdNoViolation() {
        ChatClient client = mockClient(new LlmClassifier.Response(0.5, true));

        Optional<Violation> violation = Nsfw.classifyWith(
            client, "prompt", 0.7, DEFAULT_SYSTEM_MESSAGE, "text");

        assertThat(violation).isEmpty();
    }

    @Test
    void testExceptionFailsClosed() {
        ChatClient client = mock(ChatClient.class);

        when(client.prompt()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(
            () -> Nsfw.classifyWith(client, "prompt", 0.7, DEFAULT_SYSTEM_MESSAGE, "text"))
                .isInstanceOf(GuardrailUnavailableException.class)
                .hasMessageContaining("nsfw");
    }

    @Test
    void testMissingModelChildThrows() {
        Nsfw nsfw = new Nsfw();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) nsfw.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of());

        assertThatThrownBy(() -> function.apply("any text", context))
            .isInstanceOf(MissingModelChildException.class)
            .hasMessageContaining("nsfw");
    }

    private static ChatClient mockClient(LlmClassifier.Response response) {
        ChatClient client = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(client.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenReturn(response);

        return client;
    }
}
