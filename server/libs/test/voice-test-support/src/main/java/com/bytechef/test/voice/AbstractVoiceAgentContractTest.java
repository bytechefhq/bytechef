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

package com.bytechef.test.voice;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.voice.VoiceAgentContext;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public abstract class AbstractVoiceAgentContractTest {

    protected FakeProviderWebSocket.Connector connector;
    protected RecordingWebSocketEmitter emitter;
    protected StubVoiceAgentToolset toolset;
    protected FakeProviderWebSocket socket;

    @BeforeEach
    void connect() throws Exception {
        connector = new FakeProviderWebSocket.Connector();
        emitter = new RecordingWebSocketEmitter();
        toolset = new StubVoiceAgentToolset(
            "lookupOrder", "Finds an order", "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}",
            "order 4411 shipped");

        ActionContext actionContext = Mockito.mock(ActionContext.class, Mockito.RETURNS_DEEP_STUBS);
        Context.Json json = JsonSupport.create();

        Mockito.when(actionContext.json(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Context.Json, Object> jsonFunction = invocation.getArgument(0);

                try {
                    return jsonFunction.apply(json);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });

        WebSocketHandler handler = voiceAgent(connector).apply(
            ParametersFactory.create(inputParameters()), ParametersFactory.create(connectionParameters()),
            new VoiceAgentContext(actionContext, toolset));

        handler.handle(emitter);

        socket = connector.lastSocket();
    }

    @Test
    void testSessionConfigRegistersTheTools() {
        assertThat(socket.sentTexts()).isNotEmpty();
        assertThat(socket.sentTexts()
            .getFirst()).contains(expectedToolRegistrationFragment("lookupOrder"));
    }

    @Test
    void testGreetingBecomesAssistantText() {
        providerGreets(socket, "Hi there");

        assertThat(emitter.events()).anySatisfy(event -> {
            assertThat(event).containsEntry("type", "assistant_text");
            assertThat(event).containsEntry("text", "Hi there");
        });
    }

    @Test
    void testUserSpeechBecomesTranscriptFinal() {
        providerTranscribes(socket, "where is my order");

        assertThat(emitter.events()).anySatisfy(event -> {
            assertThat(event).containsEntry("type", "transcript_final");
            assertThat(event).containsEntry("text", "where is my order");
        });
    }

    @Test
    void testInterruptionBecomesSpeechStart() {
        providerInterrupts(socket);

        assertThat(emitter.events()).anySatisfy(event -> assertThat(event).containsEntry("type", "speech_start"));
    }

    @Test
    void testProviderAudioReachesTheEmitterAsBinary() {
        byte[] pcm = {
            1, 0, 2, 0, 3, 0
        };

        providerSpeaks(socket, pcm);

        assertThat(emitter.audio()).anySatisfy(bytes -> assertThat(bytes).containsExactly(pcm));
    }

    @Test
    void testInboundAudioIsForwardedToTheProvider() {
        emitter.dispatchBinaryMessage("hello".getBytes(StandardCharsets.UTF_8));

        assertThat(socket.sentBinaries()
            .size()
            + socket.sentTexts()
                .size()).isGreaterThan(1);
    }

    @Test
    void testToolCallRunsTheToolAndRepliesToTheProvider() throws Exception {
        providerCallsTool(socket, "call-1", "lookupOrder", "{\"id\":\"4411\"}");

        for (int attempt = 0; attempt < 50 && toolset.calls()
            .isEmpty(); attempt++) {
            Thread.sleep(20);
        }

        assertThat(toolset.calls()).containsExactly("lookupOrder:{\"id\":\"4411\"}");
        assertThat(socket.sentTexts()).anySatisfy(
            text -> assertThat(text).contains(expectedToolReplyFragment("call-1", "order 4411 shipped")));
        assertThat(emitter.events()).anySatisfy(event -> assertThat(event).containsEntry("type", "tool_call"));
        assertThat(emitter.events()).anySatisfy(event -> {
            assertThat(event).containsEntry("type", "tool_result");
            assertThat(event).containsEntry("name", "lookupOrder");
            assertThat(event).containsEntry("result", "order 4411 shipped");
        });
    }

    @Test
    void testProviderErrorSurfacesOnceThenCompletes() {
        providerFails(socket);

        assertThat(emitter.error()).isNotNull();
        assertThat(socket.closed()).isTrue();
    }

    @Test
    void testProviderCloseCompletesTheEmitter() {
        socket.receiveClose(1000, "bye");

        assertThat(emitter.completed()).isTrue();
        assertThat(emitter.error()).isNull();
    }

    @Test
    void testCompletingTheEmitterClosesTheProviderSocket() {
        emitter.complete();

        assertThat(socket.closed()).isTrue();
    }

    protected abstract VoiceAgentFunction voiceAgent(ProviderWebSocketConnector connector);

    protected abstract Map<String, Object> inputParameters();

    protected abstract Map<String, Object> connectionParameters();

    protected abstract String expectedToolRegistrationFragment(String toolName);

    protected abstract void providerGreets(FakeProviderWebSocket socket, String text);

    protected abstract void providerTranscribes(FakeProviderWebSocket socket, String text);

    protected abstract void providerInterrupts(FakeProviderWebSocket socket);

    protected abstract void providerSpeaks(FakeProviderWebSocket socket, byte[] pcm);

    protected abstract void providerCallsTool(
        FakeProviderWebSocket socket, String callId, String name, String argumentsJson);

    protected abstract String expectedToolReplyFragment(String callId, String result);

    protected abstract void providerFails(FakeProviderWebSocket socket);
}
