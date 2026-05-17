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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * @author Ivica Cardic
 */
class GuardrailContextTest {

    private static final Context CONTEXT = mock(Context.class);

    @Test
    void testNullInputParametersSubstitutedWithEmpty() {
        GuardrailContext context = new GuardrailContext(
            null, ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), null, CONTEXT);

        Parameters inputParameters = context.inputParameters();

        assertThat(inputParameters).isNotNull();
    }

    @Test
    void testNullConnectionParametersSubstitutedWithEmpty() {
        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), null, ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), null, CONTEXT);

        assertThat(context.connectionParameters()).isNotNull();
    }

    @Test
    void testNullParentParametersSubstitutedWithEmpty() {
        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), null,
            ParametersFactory.create(Map.of()), Map.of(), null, CONTEXT);

        assertThat(context.parentParameters()).isNotNull();
    }

    @Test
    void testNullExtensionsSubstitutedWithEmpty() {
        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), null, Map.of(), null, CONTEXT);

        assertThat(context.extensions()).isNotNull();
    }

    @Test
    void testNullComponentConnectionsSubstitutedWithEmptyMap() {
        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), null, null, CONTEXT);

        Map<String, ComponentConnection> componentConnections = context.componentConnections();

        assertThat(componentConnections).isNotNull();
        assertThat(componentConnections).isEmpty();
    }

    @Test
    void testChatClientReturnsEmptyOptionalWhenNull() {
        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Map.of(), null, CONTEXT);

        Optional<ChatClient> chatClient = context.chatClient();

        assertThat(chatClient).isEmpty();
    }

    @Test
    void testChatClientReturnsPresentOptionalWhenProvided() {
        ChatClient mockClient = mock(ChatClient.class);

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Map.of(), mockClient, CONTEXT);

        Optional<ChatClient> chatClient = context.chatClient();

        assertThat(chatClient).isPresent();
        assertThat(chatClient.get()).isSameAs(mockClient);
    }

    @Test
    void testComponentConnectionsAreDeepCopiedAtConstruction() {
        Map<String, ComponentConnection> mutableSource = new HashMap<>();

        mutableSource.put("conn1", mock(ComponentConnection.class));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), mutableSource, null, CONTEXT);

        mutableSource.put("conn2", mock(ComponentConnection.class));

        Map<String, ComponentConnection> retrieved = context.componentConnections();

        assertThat(retrieved).hasSize(1);
        assertThat(retrieved).containsOnlyKeys("conn1");
    }

    @Test
    void testInputParametersAccessorReturnsProvidedInstance() {
        Parameters input = ParametersFactory.create(Map.of("k", "v"));

        GuardrailContext context = new GuardrailContext(
            input, ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), null, CONTEXT);

        assertThat(context.inputParameters()).isSameAs(input);
    }

    @Test
    void testNullContextRejectedAtConstruction() {
        assertThatThrownBy(() -> new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Map.of(), null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void testContextAccessorReturnsProvidedInstance() {
        Context context = mock(Context.class);

        GuardrailContext guardrailContext = new GuardrailContext(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), Map.of(), null, context);

        assertThat(guardrailContext.context()).isSameAs(context);
    }
}
