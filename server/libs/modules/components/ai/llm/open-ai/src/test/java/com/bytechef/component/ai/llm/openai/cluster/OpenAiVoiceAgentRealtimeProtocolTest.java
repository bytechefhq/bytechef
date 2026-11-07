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

package com.bytechef.component.ai.llm.openai.cluster;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OpenAiVoiceAgentRealtimeProtocolTest {

    private static final String FUNCTION_CALL_OUTPUT = "\"type\":\"function_call_output\"";
    private static final String RESPONSE_CREATE = "\"type\":\"response.create\"";
    private static final String SESSION_UPDATED = "{\"type\":\"session.updated\",\"session\":{}}";

    private final GatedVoiceAgentToolset toolset = new GatedVoiceAgentToolset();

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

        WebSocketHandler handler = OpenAiVoiceAgent.of(connector)
            .getElement()
            .apply(
                ParametersFactory.create(Map.of("outputSampleRate", 16000)),
                ParametersFactory.create(Map.of("token", "sk-test")), new VoiceAgentContext(actionContext, toolset));

        handler.handle(emitter);

        socket = connector.lastSocket();
    }

    @Test
    void testResponseCreateWaitsForResponseDoneWhenTheToolIsFast() throws InterruptedException {
        socket.receiveText(argumentsDone("call-1", "lookupOrder"));

        awaitSentCount(FUNCTION_CALL_OUTPUT, 1);

        Thread.sleep(100);

        assertThat(sentCount(RESPONSE_CREATE)).isZero();

        socket.receiveText(responseDone("call-1"));

        awaitSentCount(RESPONSE_CREATE, 1);

        assertThat(sentCount(RESPONSE_CREATE)).isEqualTo(1);
        assertThat(lastIndexOf(FUNCTION_CALL_OUTPUT)).isLessThan(lastIndexOf(RESPONSE_CREATE));
    }

    @Test
    void testResponseCreateIsSentWhenTheToolFinishesAfterResponseDone() throws InterruptedException {
        CountDownLatch gate = toolset.gate("lookupOrder");

        socket.receiveText(argumentsDone("call-1", "lookupOrder"));
        socket.receiveText(responseDone("call-1"));

        Thread.sleep(100);

        assertThat(sentCount(RESPONSE_CREATE)).isZero();

        gate.countDown();

        awaitSentCount(RESPONSE_CREATE, 1);

        assertThat(sentCount(FUNCTION_CALL_OUTPUT)).isEqualTo(1);
        assertThat(sentCount(RESPONSE_CREATE)).isEqualTo(1);
        assertThat(lastIndexOf(FUNCTION_CALL_OUTPUT)).isLessThan(lastIndexOf(RESPONSE_CREATE));
    }

    @Test
    void testOneResponseCreateOnceEveryFunctionCallOfTheResponseIsAnswered() throws InterruptedException {
        CountDownLatch slowGate = toolset.gate("cancelOrder");

        socket.receiveText(argumentsDone("call-1", "lookupOrder"));
        socket.receiveText(argumentsDone("call-2", "cancelOrder"));

        awaitSentCount(FUNCTION_CALL_OUTPUT, 1);

        socket.receiveText(responseDone("call-1", "call-2"));

        Thread.sleep(100);

        assertThat(sentCount(RESPONSE_CREATE)).isZero();

        slowGate.countDown();

        awaitSentCount(RESPONSE_CREATE, 1);

        Thread.sleep(100);

        assertThat(sentCount(FUNCTION_CALL_OUTPUT)).isEqualTo(2);
        assertThat(sentCount(RESPONSE_CREATE)).isEqualTo(1);
    }

    @Test
    void testRecoverableErrorKeepsTheSessionOpen() {
        socket.receiveText(SESSION_UPDATED);
        socket.receiveText(
            "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\"," +
                "\"code\":\"conversation_already_has_active_response\",\"message\":\"busy\"}}");

        assertThat(emitter.error()).isNull();
        assertThat(emitter.completed()).isFalse();
        assertThat(socket.closed()).isFalse();
    }

    @Test
    void testInvalidValueForTheSessionUpdateIsFatal() {
        socket.receiveText(SESSION_UPDATED);
        socket.receiveText(
            "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"code\":\"invalid_value\"," +
                "\"message\":\"bad voice\",\"event_id\":\"" + sessionUpdateEventId() + "\"}}");

        assertThat(emitter.error()).hasMessage("bad voice");
        assertThat(socket.closed()).isTrue();
    }

    @Test
    void testErrorBeforeTheSessionIsUpdatedIsFatal() {
        socket.receiveText(
            "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"code\":\"invalid_value\"," +
                "\"message\":\"bad model\"}}");

        assertThat(emitter.error()).hasMessage("bad model");
        assertThat(socket.closed()).isTrue();
    }

    @Test
    void testInvalidValueForALaterEventStaysRecoverable() {
        socket.receiveText(SESSION_UPDATED);
        socket.receiveText(
            "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"code\":\"invalid_value\"," +
                "\"message\":\"bad item\",\"event_id\":\"evt-later\"}}");

        assertThat(emitter.error()).isNull();
        assertThat(emitter.completed()).isFalse();
        assertThat(socket.closed()).isFalse();
    }

    @Test
    void testResponseCancelNotActiveEndsTheSession() {
        socket.receiveText(SESSION_UPDATED);
        socket.receiveText(
            "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\"," +
                "\"code\":\"response_cancel_not_active\",\"message\":\"not active\"}}");

        assertThat(emitter.error()).hasMessage("not active");
        assertThat(socket.closed()).isTrue();
    }

    @Test
    void testUnknownErrorEndsTheSession() {
        socket.receiveText(
            "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"code\":\"session_expired\"," +
                "\"message\":\"expired\"}}");

        assertThat(emitter.error()).hasMessage("expired");
        assertThat(socket.closed()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSessionUpdateAlwaysAsksForTwentyFourKilohertzOutput() {
        Map<String, Object> sessionUpdate = (Map<String, Object>) JsonSupport.create()
            .read(
                socket.sentTexts()
                    .getFirst(),
                Map.class);
        Map<String, Object> session = (Map<String, Object>) sessionUpdate.get("session");
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> output = (Map<String, Object>) audio.get("output");
        Map<String, Object> format = (Map<String, Object>) output.get("format");

        assertThat(format).containsEntry("rate", 24000);
    }

    @SuppressWarnings("unchecked")
    private String sessionUpdateEventId() {
        Map<String, Object> sessionUpdate = (Map<String, Object>) JsonSupport.create()
            .read(
                socket.sentTexts()
                    .getFirst(),
                Map.class);

        assertThat(sessionUpdate).containsEntry("type", "session.update");
        assertThat(sessionUpdate.get("event_id")).isInstanceOf(String.class);

        return (String) sessionUpdate.get("event_id");
    }

    private static String argumentsDone(String callId, String name) {
        return "{\"type\":\"response.function_call_arguments.done\",\"response_id\":\"resp-1\",\"call_id\":\"" +
            callId + "\",\"name\":\"" + name + "\",\"arguments\":\"{}\"}";
    }

    private static String responseDone(String... callIds) {
        StringBuilder output = new StringBuilder();

        for (String callId : callIds) {
            if (!output.isEmpty()) {
                output.append(',');
            }

            output.append("{\"type\":\"function_call\",\"call_id\":\"")
                .append(callId)
                .append("\"}");
        }

        return "{\"type\":\"response.done\",\"response\":{\"id\":\"resp-1\",\"output\":[" + output + "]}}";
    }

    private void awaitSentCount(String fragment, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);

        while (sentCount(fragment) < count) {
            assertThat(System.nanoTime()).as("'%s' sent %d time(s) in time", fragment, count)
                .isLessThan(deadline);

            Thread.sleep(10);
        }
    }

    private int sentCount(String fragment) {
        return (int) socket.sentTexts()
            .stream()
            .filter(text -> text.contains(fragment))
            .count();
    }

    private int lastIndexOf(String fragment) {
        List<String> sentTexts = socket.sentTexts();

        for (int index = sentTexts.size() - 1; index >= 0; index--) {
            if (sentTexts.get(index)
                .contains(fragment)) {

                return index;
            }
        }

        return -1;
    }

    private static final class GatedVoiceAgentToolset implements VoiceAgentToolset {

        private final Map<String, CountDownLatch> gates = new ConcurrentHashMap<>();

        CountDownLatch gate(String name) {
            CountDownLatch gate = new CountDownLatch(1);

            gates.put(name, gate);

            return gate;
        }

        @Override
        public List<VoiceToolDefinition> definitions() {
            return List.of(
                new VoiceToolDefinition("lookupOrder", "Finds an order", "{\"type\":\"object\"}"),
                new VoiceToolDefinition("cancelOrder", "Cancels an order", "{\"type\":\"object\"}"));
        }

        @Override
        public String call(String name, String argumentsJson) {
            CountDownLatch gate = gates.get(name);

            if (gate != null) {
                try {
                    if (!gate.await(5, TimeUnit.SECONDS)) {
                        return "timed out";
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    return "interrupted";
                }
            }

            return "result of " + name;
        }
    }
}
