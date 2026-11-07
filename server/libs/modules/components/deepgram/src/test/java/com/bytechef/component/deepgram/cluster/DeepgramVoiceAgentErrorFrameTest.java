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
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.test.voice.FakeProviderWebSocket;
import com.bytechef.test.voice.JsonSupport;
import com.bytechef.test.voice.RecordingWebSocketEmitter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DeepgramVoiceAgentErrorFrameTest {

    private RecordingWebSocketEmitter emitter;
    private FakeProviderWebSocket socket;

    @BeforeEach
    void connect() throws Exception {
        FakeProviderWebSocket.Connector connector = new FakeProviderWebSocket.Connector();
        ActionContext actionContext = Mockito.mock(ActionContext.class, Mockito.RETURNS_DEEP_STUBS);
        Context.Json json = JsonSupport.create();

        Mockito.when(actionContext.json(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Context.Json, Object> jsonFunction = invocation.getArgument(0);

                return jsonFunction.apply(json);
            });

        emitter = new RecordingWebSocketEmitter();

        WebSocketHandler handler = DeepgramVoiceAgent.of(connector)
            .getElement()
            .apply(
                ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of("token", "dg-key")),
                new VoiceAgentContext(actionContext, VoiceAgentToolset.EMPTY));

        handler.handle(emitter);

        socket = connector.lastSocket();
    }

    @Test
    void testErrorFrameFailsTheEmitterAndClosesTheProviderSocket() {
        socket.receiveText("{\"type\":\"Error\",\"description\":\"Invalid settings\",\"code\":\"INVALID_SETTINGS\"}");

        assertThat(emitter.error()).hasMessage("Invalid settings");
        assertThat(emitter.completed()).isTrue();
        assertThat(socket.closed()).isTrue();
        assertThat(emitter.events()).noneSatisfy(event -> assertThat(event).containsKey("source"));
    }

    @Test
    void testErrorFrameWithoutADescriptionStillFailsTheEmitter() {
        socket.receiveText("{\"type\":\"Error\"}");

        assertThat(emitter.error()).isNotNull();
        assertThat(socket.closed()).isTrue();
    }
}
