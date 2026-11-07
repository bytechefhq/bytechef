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

package com.bytechef.component.elevenlabs.cluster;

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
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ElevenLabsVoiceAgentResamplingTest {

    @Test
    void testInputIsResampledDownToTheDefaultProviderRateWhenNoMetadataArrived() throws Exception {
        byte[] pcm = pcmOf(300);

        FakeProviderWebSocket socket = start(Map.of("agentId", "agent-1", "inputSampleRate", 24000));

        dispatchAudio(pcm);

        byte[] sentAudio = lastSentAudio(socket);

        assertThat(sentAudio).hasSize(pcm.length * 2 / 3);
    }

    @Test
    void testInputIsUnchangedWhenMetadataMatchesTheConfiguredInputRate() throws Exception {
        byte[] pcm = pcmOf(200);

        FakeProviderWebSocket socket = start(Map.of("agentId", "agent-1"));

        receiveConversationInitiationMetadata(socket, "pcm_16000");
        dispatchAudio(pcm);

        byte[] sentAudio = lastSentAudio(socket);

        assertThat(sentAudio).isEqualTo(pcm);
    }

    @Test
    void testInputIsUpsampledWhenMetadataReportsAHigherProviderRate() throws Exception {
        byte[] pcm = pcmOf(200);

        FakeProviderWebSocket socket = start(Map.of("agentId", "agent-1"));

        receiveConversationInitiationMetadata(socket, "pcm_24000");
        dispatchAudio(pcm);

        byte[] sentAudio = lastSentAudio(socket);

        assertThat(sentAudio).hasSize(pcm.length * 3 / 2);
    }

    private RecordingWebSocketEmitter emitter;

    private static byte[] pcmOf(int sampleCount) {
        byte[] pcm = new byte[sampleCount * 2];

        for (int index = 0; index < pcm.length; index++) {
            pcm[index] = (byte) (index % 128);
        }

        return pcm;
    }

    private static void receiveConversationInitiationMetadata(FakeProviderWebSocket socket, String inputFormat) {
        socket.receiveText(
            "{\"type\":\"conversation_initiation_metadata\",\"conversation_initiation_metadata_event\":{" +
                "\"agent_output_audio_format\":\"pcm_16000\",\"user_input_audio_format\":\"" + inputFormat +
                "\"}}");
    }

    private void dispatchAudio(byte[] pcm) {
        emitter.dispatchBinaryMessage(pcm);
    }

    private static final Pattern USER_AUDIO_CHUNK_PATTERN =
        Pattern.compile("\"user_audio_chunk\"\\s*:\\s*\"([^\"]*)\"");

    private static byte[] lastSentAudio(FakeProviderWebSocket socket) {
        String lastText = socket.sentTexts()
            .getLast();
        Matcher matcher = USER_AUDIO_CHUNK_PATTERN.matcher(lastText);

        if (!matcher.find()) {
            throw new AssertionError("No user_audio_chunk field found in: " + lastText);
        }

        return Base64.getDecoder()
            .decode(matcher.group(1));
    }

    private FakeProviderWebSocket start(Map<String, Object> inputParameters) throws Exception {
        FakeProviderWebSocket.Connector connector = new FakeProviderWebSocket.Connector();

        emitter = new RecordingWebSocketEmitter();

        ActionContext actionContext = Mockito.mock(ActionContext.class, Mockito.RETURNS_DEEP_STUBS);
        Context.Json json = JsonSupport.create();

        Mockito.when(actionContext.json(Mockito.any()))
            .thenAnswer(invocation -> {
                Context.ContextFunction<Context.Json, Object> jsonFunction = invocation.getArgument(0);

                return jsonFunction.apply(json);
            });

        WebSocketHandler handler = ElevenLabsVoiceAgent
            .of(
                connector,
                (agentId, apiKey) -> "wss://api.elevenlabs.io/v1/convai/conversation?agent_id=" + agentId)
            .getElement()
            .apply(
                ParametersFactory.create(inputParameters),
                ParametersFactory.create(Map.of("key", "xi-api-key", "value", "el-key")),
                new VoiceAgentContext(actionContext, VoiceAgentToolset.EMPTY));

        handler.handle(emitter);

        return connector.lastSocket();
    }
}
