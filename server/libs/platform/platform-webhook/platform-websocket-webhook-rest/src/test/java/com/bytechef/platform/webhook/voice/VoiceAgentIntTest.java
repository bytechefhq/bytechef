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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.voice.VoiceSessionEngine.VoiceSession;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Drives a voice session end to end through the real {@link VoiceSessionEngine}, the real
 * {@link WebSocketEmitterRegistry} and the real {@link WebSocketTaskChain}. Only the components are faked, at the
 * {@link ActionDefinitionFacade} seam where the platform hands off to a vendor.
 *
 * <p>
 * The multi-stage case is the one that matters: an STT → agent → TTS pipeline could not run at all while the pipeline
 * was dispatched as Atlas tasks, because a stage never completes until the caller hangs up and Atlas would not start
 * the second stage until the first completed. Nothing sequences the stages here.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class VoiceAgentIntTest {

    private static final long TIMEOUT_SECONDS = 5;

    private final ConcurrentLinkedQueue<byte[]> outboundAudio = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> terminalCancels = new ConcurrentLinkedQueue<>();
    private final CountDownLatch audioReceived = new CountDownLatch(1);
    private final CountDownLatch cancelReceived = new CountDownLatch(1);
    private final AtomicReference<Map<String, ?>> agentInputParameters = new AtomicReference<>();

    private ActionDefinitionFacade actionDefinitionFacade;
    private VoiceSessionEngine voiceSessionEngine;
    private WebSocketEmitterRegistry webSocketEmitterRegistry;

    @BeforeEach
    void beforeEach() {
        actionDefinitionFacade = mock(ActionDefinitionFacade.class);
        webSocketEmitterRegistry = new WebSocketEmitterRegistry();

        voiceSessionEngine = new VoiceSessionEngine(
            actionDefinitionFacade, SpelEvaluator.create(), webSocketEmitterRegistry,
            new WebSocketTaskChain(webSocketEmitterRegistry));

        when(
            actionDefinitionFacade.executePerform(
                anyString(), anyInt(), anyString(), nullable(Long.class), nullable(Long.class), nullable(Long.class),
                nullable(Long.class), nullable(String.class), any(), any(), any(), nullable(Long.class),
                nullable(PlatformType.class), anyBoolean(), nullable(Map.class),
                nullable(Map.class), nullable(Instant.class)))
                    .thenAnswer(invocation -> createStage(invocation.getArgument(2), invocation.getArgument(8)));
    }

    @Test
    void testMultiStagePipelineCarriesAudioAllTheWayThrough() throws Exception {
        VoiceSession voiceSession = start("""
            {"tasks":[
              {"name":"stt","type":"fake/v1/transcribe/v1"},
              {"name":"agent","type":"fake/v1/chat/v1"},
              {"name":"tts","type":"fake/v1/speak/v1"}
            ]}""");

        assertThat(voiceSession.stageNames()).containsExactly("stt", "agent", "tts");

        firstStageEmitter(voiceSession).dispatchBinaryMessage("hello there".getBytes(StandardCharsets.UTF_8));

        assertThat(audioReceived.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("synthesized audio should reach the caller within %ds", TIMEOUT_SECONDS)
            .isTrue();

        assertThat(new String(outboundAudio.peek(), StandardCharsets.UTF_8)).isEqualTo("you said: hello there");
    }

    @Test
    void testSingleStagePipelineCarriesAudioAllTheWayThrough() throws Exception {
        VoiceSession voiceSession = start("{\"tasks\":[{\"name\":\"agent\",\"type\":\"fake/v1/voiceAgent/v1\"}]}");

        firstStageEmitter(voiceSession).dispatchBinaryMessage("hello there".getBytes(StandardCharsets.UTF_8));

        assertThat(audioReceived.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

        assertThat(new String(outboundAudio.peek(), StandardCharsets.UTF_8)).isEqualTo("you said: hello there");
    }

    @Test
    void testBargeInCancelReachesTheCallerAndEveryOtherStage() throws Exception {
        VoiceSession voiceSession = start("""
            {"tasks":[
              {"name":"stt","type":"fake/v1/transcribe/v1"},
              {"name":"agent","type":"fake/v1/chat/v1"},
              {"name":"tts","type":"fake/v1/speak/v1"}
            ]}""");

        // Speech-start mid-reply: the STT stage cancels the turn, and every other stage plus the caller's session
        // must hear about it — that is what stops the assistant talking over the caller.
        firstStageEmitter(voiceSession).dispatchBinaryMessage("__speech_start__".getBytes(StandardCharsets.UTF_8));

        assertThat(cancelReceived.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            .as("cancelTurn should reach the caller's session within %ds", TIMEOUT_SECONDS)
            .isTrue();

        assertThat(terminalCancels).contains("turn-1");
    }

    @Test
    void testStageParametersAreEvaluatedAgainstSessionInputs() {
        start("""
            {"tasks":[
              {"name":"agent","type":"fake/v1/voiceAgent/v1","parameters":{"greeting":"calling ${callSid}"}}
            ]}""");

        // No job runs during a call, so nothing else would resolve ${callSid} for the stage.
        assertThat(agentInputParameters.get())
            .extracting(parameters -> parameters.get("greeting"))
            .isEqualTo("calling CA-test-1");
    }

    @Test
    void testStoppingTheSessionReleasesEveryStage() {
        VoiceSession voiceSession = start("""
            {"tasks":[
              {"name":"stt","type":"fake/v1/transcribe/v1"},
              {"name":"agent","type":"fake/v1/chat/v1"}
            ]}""");

        voiceSessionEngine.stop(voiceSession.sessionId());

        for (String stageName : voiceSession.stageNames()) {
            assertThat(webSocketEmitterRegistry.get(voiceSession.sessionId(), stageName))
                .as("stage %s should be unregistered once the session ends", stageName)
                .isEmpty();
        }
    }

    private VoiceSession start(String pipelineDefinition) {
        return voiceSessionEngine.start(
            pipelineDefinition, Map.of("callSid", "CA-test-1"), null, null, this::attachTerminalBridge);
    }

    private WebSocketEmitter firstStageEmitter(VoiceSession voiceSession) {
        return webSocketEmitterRegistry.get(voiceSession.sessionId(), voiceSession.stageNames()
            .getFirst())
            .orElseThrow();
    }

    /**
     * Stands in for the caller's WebSocket session.
     */
    private void attachTerminalBridge(WebSocketEmitter emitter) {
        emitter.addOutboundBinaryListener(bytes -> {
            outboundAudio.add(bytes);

            audioReceived.countDown();
        });

        emitter.addOutboundTurnCancelListener(turnId -> {
            terminalCancels.add(turnId);

            cancelReceived.countDown();
        });
    }

    private WebSocketHandler createStage(String actionName, Map<String, ?> inputParameters) {
        return switch (actionName) {
            case "transcribe" -> createTranscribeStage();
            case "chat" -> createChatStage();
            case "speak" -> createSpeakStage();
            case "voiceAgent" -> {
                agentInputParameters.set(inputParameters);

                yield createVoiceAgentStage();
            }
            default -> throw new IllegalArgumentException("Unexpected stage action: " + actionName);
        };
    }

    /**
     * Fake STT: caller audio in, a finalized user turn out. Speech-start cancels the turn in flight.
     */
    private static WebSocketHandler createTranscribeStage() {
        return emitter -> emitter.addBinaryMessageListener(audio -> {
            String transcript = new String(audio, StandardCharsets.UTF_8);

            if ("__speech_start__".equals(transcript)) {
                emitter.cancelTurn("turn-1");

                return;
            }

            emitter.send(JsonUtils.write(Map.of("type", "user_turn", "text", transcript, "turnId", "turn-1")));
        });
    }

    /**
     * Fake agent: a user turn in, an assistant reply out.
     */
    private static WebSocketHandler createChatStage() {
        return emitter -> emitter.addMessageListener(message -> {
            Map<?, ?> event = (Map<?, ?>) JsonUtils.read(String.valueOf(message), Map.class);

            if (!"user_turn".equals(event.get("type"))) {
                return;
            }

            emitter.send(
                JsonUtils.write(
                    Map.of("type", "assistant_token", "text", "you said: " + event.get("text"), "turnId", "turn-1")));
        });
    }

    /**
     * Fake TTS: an assistant reply in, audio out to the caller.
     */
    private static WebSocketHandler createSpeakStage() {
        return emitter -> emitter.addMessageListener(message -> {
            Map<?, ?> event = (Map<?, ?>) JsonUtils.read(String.valueOf(message), Map.class);

            if (!"assistant_token".equals(event.get("type"))) {
                return;
            }

            emitter.sendBinary(String.valueOf(event.get("text"))
                .getBytes(StandardCharsets.UTF_8));
        });
    }

    /**
     * Fake all-in-one agent, the {@code deepgram/v1/voiceAgent} shape: one component terminates the whole loop.
     */
    private static WebSocketHandler createVoiceAgentStage() {
        return emitter -> emitter.addBinaryMessageListener(audio -> {
            String transcript = new String(audio, StandardCharsets.UTF_8);

            if ("__speech_start__".equals(transcript)) {
                emitter.cancelTurn("turn-1");

                return;
            }

            emitter.sendBinary(("you said: " + transcript).getBytes(StandardCharsets.UTF_8));
        });
    }

}
