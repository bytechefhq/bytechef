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

package com.bytechef.platform.webhook.voice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.voice.VoiceAgentContext;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolsetFactory;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class VoiceSessionEngineTest {

    private static final long TIMEOUT_SECONDS = 5;

    private final ConcurrentLinkedQueue<byte[]> outboundAudio = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Object> outboundEvents = new ConcurrentLinkedQueue<>();
    private final CountDownLatch audioReceived = new CountDownLatch(1);
    private final AtomicReference<Map<String, ?>> agentInputParameters = new AtomicReference<>();
    private final AtomicReference<VoiceAgentContext> agentContext = new AtomicReference<>();

    private ClusterElementDefinitionService clusterElementDefinitionService;
    private ContextFactory contextFactory;
    private ObjectProvider<VoiceAgentToolsetFactory> toolsetFactoryProvider;
    private VoiceSessionEngine voiceSessionEngine;
    private WebSocketEmitterRegistry webSocketEmitterRegistry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        webSocketEmitterRegistry = new WebSocketEmitterRegistry();

        contextFactory = mock(ContextFactory.class);

        when(contextFactory.createActionContext(
            anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(mock(ActionContext.class));

        ObjectProvider<ContextFactory> contextFactoryProvider = mock(ObjectProvider.class);

        when(contextFactoryProvider.getIfAvailable()).thenReturn(contextFactory);

        toolsetFactoryProvider = mock(ObjectProvider.class);

        when(toolsetFactoryProvider.getIfAvailable()).thenReturn((extensions, connections, context) -> new Toolset());

        voiceSessionEngine = new VoiceSessionEngine(
            clusterElementDefinitionService, contextFactoryProvider, mock(ConnectionService.class),
            SpelEvaluator.create(), webSocketEmitterRegistry, toolsetFactoryProvider);

        VoiceAgentFunction fakeVoiceAgent = (inputParameters, connectionParameters, context) -> {
            agentInputParameters.set(inputParameters.toMap());
            agentContext.set(context);

            return createVoiceAgentStage(context);
        };

        when(clusterElementDefinitionService.getClusterElement(eq("fake"), eq(1), eq("voiceAgent")))
            .thenReturn(fakeVoiceAgent);
    }

    @Test
    void testSingleElementCarriesAudioAllTheWayThrough() throws Exception {
        VoiceSession voiceSession = start("{\"greeting\":\"hi\"}");

        voiceSession.emitter()
            .dispatchBinaryMessage("hello there".getBytes(StandardCharsets.UTF_8));

        assertThat(audioReceived.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(new String(outboundAudio.peek(), StandardCharsets.UTF_8)).isEqualTo("you said: hello there");
    }

    @Test
    void testElementParametersAreEvaluatedAgainstSessionInputs() {
        start("{\"greeting\":\"session ${sessionId}\"}");

        assertThat(agentInputParameters.get())
            .extracting(parameters -> parameters.get("greeting"))
            .isEqualTo("session S-1");
    }

    @Test
    void testElementReceivesTheToolsetFromTheFactory() {
        start("{}");

        assertThat(agentContext.get()
            .toolset()
            .definitions())
                .extracting(VoiceToolDefinition::name)
                .containsExactly("lookupOrder");
    }

    @Test
    void testTranscriptCollectsNormalisedEventsAndToolCalls() {
        VoiceSession voiceSession = start("{}");

        voiceSession.emitter()
            .dispatchBinaryMessage("__tool__".getBytes(StandardCharsets.UTF_8));

        assertThat(voiceSession.transcript()
            .entries())
                .extracting(entry -> entry.get("role"), entry -> entry.get("text"))
                .contains(Tuple.tuple("user", "__tool__"), Tuple.tuple("assistant", "ordered"));
        assertThat(voiceSession.transcript()
            .toolCalls())
                .singleElement()
                .satisfies(toolCall -> {
                    assertThat(toolCall.get("name")).isEqualTo("lookupOrder");
                    assertThat(toolCall.get("arguments")).isEqualTo(Map.of("id", "4411"));
                    assertThat(toolCall.get("result")).isEqualTo("order 4411 shipped");
                });
        assertThat(outboundEvents).hasSize(4);
    }

    @Test
    void testJobPrincipalAndEnvironmentReachTheActionContext() {
        voiceSessionEngine.start(
            triggerExtensions("{}"), Map.of(), Map.of("sessionId", "S-1"), 7L, 70L, 3L, PlatformType.AUTOMATION, false,
            this::attachBridge);

        verify(contextFactory).createActionContext(
            eq("fake"), eq(1), eq("voiceAgent"), eq(7L), eq(70L), isNull(), isNull(), isNull(), isNull(), eq(3L),
            eq(PlatformType.AUTOMATION), eq(false));
    }

    @Test
    void testTranscriptEventsWithoutTextAreNotRecorded() {
        VoiceSession voiceSession = start("{}");

        WebSocketEmitter emitter = voiceSession.emitter();

        emitter.send(Map.of("type", "transcript_final"));
        emitter.send(Map.of("type", "assistant_text"));
        emitter.send(Map.of("type", "assistant_text", "text", "hello"));

        assertThat(voiceSession.transcript()
            .entries())
                .extracting(entry -> entry.get("text"))
                .containsExactly("hello");
    }

    @Test
    void testOutputSampleRateIsReadFromTheElement() {
        assertThat(start("{\"outputSampleRate\":16000}").outputSampleRate()).isEqualTo(16000);
        assertThat(start("{}").outputSampleRate()).isEqualTo(24000);
    }

    @Test
    void testMissingVoiceAgentElementFailsFast() {
        assertThatThrownBy(() -> voiceSessionEngine.start(
            Map.of(), Map.of(), Map.of("sessionId", "S-1"), null, null, null, null, false,
            emitter -> assertThat(emitter).isNotNull()))
                .isInstanceOf(VoiceSessionEngine.NoVoiceAgentException.class)
                .hasMessageStartingWith(VoiceSessionEngine.NO_VOICE_AGENT_MESSAGE);
    }

    @Test
    void testToolsBesideTheVoiceAgentOnTheTriggerReachTheFactory() {
        AtomicReference<Map<String, ?>> capturedExtensions = new AtomicReference<>();

        when(toolsetFactoryProvider.getIfAvailable()).thenReturn((extensions, connections, context) -> {
            capturedExtensions.set(extensions);

            return new Toolset();
        });

        Map<?, ?> parameters = JsonUtils.read("{}", Map.class);

        Map<String, ?> triggerExtensions = Map.of(
            "clusterElements", Map.of(
                "voiceAgent", Map.of(
                    "name", "voiceAgent_1", "type", "fake/v1/voiceAgent", "parameters", parameters),
                "tools", List.of(
                    Map.of("name", "tools_1", "type", "slack/v1/sendMessage", "parameters", Map.of()))));

        voiceSessionEngine.start(
            triggerExtensions, Map.of(), Map.of("sessionId", "S-1"), null, null, null, null, false, this::attachBridge);

        ClusterElementMap capturedClusterElementMap = ClusterElementMap.of(capturedExtensions.get());

        assertThat(capturedClusterElementMap.getClusterElements(BaseToolFunction.TOOLS))
            .extracting(ClusterElement::getWorkflowNodeName)
            .containsExactly("tools_1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMissingContextFactoryFailsWithAClearError() {
        ObjectProvider<ContextFactory> contextFactoryProvider = mock(ObjectProvider.class);

        when(contextFactoryProvider.getIfAvailable()).thenReturn(null);

        VoiceSessionEngine engineWithoutContextFactory = new VoiceSessionEngine(
            clusterElementDefinitionService, contextFactoryProvider, mock(ConnectionService.class),
            SpelEvaluator.create(), webSocketEmitterRegistry, toolsetFactoryProvider);

        assertThatThrownBy(() -> engineWithoutContextFactory.start(
            triggerExtensions("{}"), Map.of(), Map.of("sessionId", "S-1"), null, null, null, null, false,
            this::attachBridge))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Voice sessions are not available on this node: no ContextFactory");
        assertThat(engineWithoutContextFactory.emitter(1L)).isEmpty();
    }

    @Test
    void testProviderErrorEmitsExactlyOneErrorEvent() {
        VoiceSession voiceSession = start("{}");

        WebSocketEmitter emitter = voiceSession.emitter();

        emitter.error(new RuntimeException("provider down"));
        emitter.error(new RuntimeException("provider down again"));

        assertThat(outboundEvents)
            .singleElement()
            .isEqualTo(Map.of("type", "error", "message", "provider down"));
    }

    @Test
    void testBridgeThatThrowsLeavesNoEmitterRegistered() {
        assertThatThrownBy(() -> voiceSessionEngine.start(
            triggerExtensions("{}"), Map.of(), Map.of("sessionId", "S-1"), null, null, null, null, false,
            emitter -> {
                throw new IllegalStateException("bridge down");
            }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(voiceSessionEngine.emitter(1L)).isEmpty();
    }

    @Test
    void testToolsAsAListBesideTheVoiceAgentReachTheFactory() {
        AtomicReference<Map<String, ?>> capturedExtensions = new AtomicReference<>();

        when(toolsetFactoryProvider.getIfAvailable()).thenReturn((extensions, connections, context) -> {
            capturedExtensions.set(extensions);

            return new Toolset();
        });

        Map<String, ?> triggerExtensions = Map.of(
            "clusterElements", Map.of(
                "voiceAgent", Map.of(
                    "name", "voiceAgent_1", "type", "fake/v1/voiceAgent", "parameters", Map.of()),
                "tools", List.of(
                    Map.of("name", "tools_1", "type", "slack/v1/sendMessage", "parameters", Map.of()),
                    Map.of("name", "tools_2", "type", "shopify/v1/getOrder", "parameters", Map.of()))));

        voiceSessionEngine.start(
            triggerExtensions, Map.of(), Map.of("sessionId", "S-1"), null, null, null, null, false,
            this::attachBridge);

        ClusterElementMap capturedClusterElementMap = ClusterElementMap.of(capturedExtensions.get());

        assertThat(capturedClusterElementMap.getClusterElements(BaseToolFunction.TOOLS))
            .extracting(ClusterElement::getWorkflowNodeName)
            .containsExactly("tools_1", "tools_2");
    }

    @Test
    void testStoppingTheSessionReleasesTheEmitter() {
        VoiceSession voiceSession = start("{}");

        assertThat(voiceSessionEngine.emitter(voiceSession.sessionId())).isPresent();

        voiceSessionEngine.stop(voiceSession.sessionId());

        assertThat(voiceSessionEngine.emitter(voiceSession.sessionId())).isEmpty();
    }

    private VoiceSession start(String parametersJson) {
        return voiceSessionEngine.start(
            triggerExtensions(parametersJson), Map.of(), Map.of("sessionId", "S-1"), null, null, null, null, false,
            this::attachBridge);
    }

    private static Map<String, ?> triggerExtensions(String parametersJson) {
        Map<?, ?> parameters = JsonUtils.read(parametersJson, Map.class);

        return Map.of(
            "clusterElements", Map.of(
                "voiceAgent", Map.of(
                    "name", "voiceAgent_1", "type", "fake/v1/voiceAgent", "parameters", parameters)));
    }

    private void attachBridge(WebSocketEmitter emitter) {
        emitter.addOutboundBinaryListener(bytes -> {
            outboundAudio.add(bytes);
            audioReceived.countDown();
        });
        emitter.addOutboundListener(outboundEvents::add);
    }

    private static WebSocketHandler createVoiceAgentStage(VoiceAgentContext context) {
        return emitter -> emitter.addBinaryMessageListener(audio -> {
            String transcript = new String(audio, StandardCharsets.UTF_8);

            if ("__tool__".equals(transcript)) {
                String result = context.toolset()
                    .call("lookupOrder", "{\"id\":\"4411\"}");

                emitter.send(Map.of("type", "transcript_final", "text", transcript));
                emitter.send(Map.of("type", "tool_call", "name", "lookupOrder", "arguments", Map.of("id", "4411")));
                emitter.send(
                    Map.of(
                        "type", "tool_result", "name", "lookupOrder", "ok", true, "arguments", Map.of("id", "4411"),
                        "result", result));
                emitter.send(Map.of("type", "assistant_text", "text", "ordered"));

                return;
            }

            emitter.sendBinary(("you said: " + transcript).getBytes(StandardCharsets.UTF_8));
        });
    }

    private static final class Toolset implements VoiceAgentToolset {

        @Override
        public List<VoiceToolDefinition> definitions() {
            return List.of(new VoiceToolDefinition("lookupOrder", "Looks up an order", "{\"type\":\"object\"}"));
        }

        @Override
        public String call(String name, String argumentsJson) {
            return "order 4411 shipped";
        }
    }
}
