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

package com.bytechef.component.deepgram.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.voice.VoiceAgentContext;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.voice.FakeProviderWebSocket;
import com.bytechef.test.voice.JsonSupport;
import com.bytechef.test.voice.RecordingWebSocketEmitter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DeepgramVoiceAgentToolFailureTest {

    @Test
    void testToolCallStillAnswersWhenTheToolThrows() throws Exception {
        FakeProviderWebSocket.Connector connector = new FakeProviderWebSocket.Connector();
        RecordingWebSocketEmitter emitter = new RecordingWebSocketEmitter();
        ThrowingVoiceAgentToolset toolset = new ThrowingVoiceAgentToolset();

        ActionContext actionContext = Mockito.mock(ActionContext.class, Mockito.RETURNS_DEEP_STUBS);
        Context.Json json = JsonSupport.create();

        Mockito.when(actionContext.json(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Context.Json, Object> jsonFunction = invocation.getArgument(0);

                return jsonFunction.apply(json);
            });

        WebSocketHandler handler = DeepgramVoiceAgent.of(connector)
            .getElement()
            .apply(
                ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("token", "dg-key")),
                new VoiceAgentContext(actionContext, toolset));

        handler.handle(emitter);

        FakeProviderWebSocket socket = connector.lastSocket();

        socket.receiveText(
            "{\"type\":\"FunctionCallRequest\",\"functions\":[{\"id\":\"call-1\",\"name\":\"lookupOrder\"," +
                "\"arguments\":\"{}\",\"client_side\":true}]}");

        for (int attempt = 0; attempt < 50 && socket.sentTexts()
            .size() < 2; attempt++) {

            Thread.sleep(20);
        }

        assertThat(emitter.events()).anySatisfy(event -> {
            assertThat(event).containsEntry("type", "tool_result");
            assertThat(event).containsEntry("name", "lookupOrder");
            assertThat(event).containsEntry("ok", false);
            assertThat(event).containsEntry("result", "boom");
        });
        assertThat(socket.sentTexts()).anySatisfy(text -> {
            assertThat(text).contains("\"type\":\"FunctionCallResponse\",\"id\":\"call-1\"");
            assertThat(text).contains("\"content\":\"boom\"");
        });
    }

    private static final class ThrowingVoiceAgentToolset implements VoiceAgentToolset {

        @Override
        public List<VoiceToolDefinition> definitions() {
            return List.of(new VoiceToolDefinition("lookupOrder", "Finds an order", "{\"type\":\"object\"}"));
        }

        @Override
        public String call(String name, String argumentsJson) {
            throw new IllegalStateException("boom");
        }
    }
}
