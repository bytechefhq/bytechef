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

import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.test.voice.AbstractVoiceAgentContractTest;
import com.bytechef.test.voice.FakeProviderWebSocket;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ElevenLabsVoiceAgentContractTest extends AbstractVoiceAgentContractTest {

    @Override
    protected VoiceAgentFunction voiceAgent(ProviderWebSocketConnector connector) {
        return ElevenLabsVoiceAgent.of(
            connector, (agentId, apiKey) -> "wss://api.elevenlabs.io/v1/convai/conversation?agent_id=" + agentId)
            .getElement();
    }

    @Override
    protected Map<String, Object> inputParameters() {
        return Map.of("agentId", "agent-1");
    }

    @Override
    protected Map<String, Object> connectionParameters() {
        return Map.of("key", "xi-api-key", "value", "el-key");
    }

    @Override
    protected String expectedToolRegistrationFragment(String toolName) {
        return "\"type\":\"conversation_initiation_client_data\"";
    }

    @Override
    protected void providerGreets(FakeProviderWebSocket socket, String text) {
        socket.receiveText(
            "{\"type\":\"agent_response\",\"agent_response_event\":{\"agent_response\":\"" + text + "\"}}");
    }

    @Override
    protected void providerTranscribes(FakeProviderWebSocket socket, String text) {
        socket.receiveText(
            "{\"type\":\"user_transcript\",\"user_transcription_event\":{\"user_transcript\":\"" + text + "\"}}");
    }

    @Override
    protected void providerInterrupts(FakeProviderWebSocket socket) {
        socket.receiveText("{\"type\":\"interruption\",\"interruption_event\":{\"event_id\":1}}");
    }

    @Override
    protected void providerSpeaks(FakeProviderWebSocket socket, byte[] pcm) {
        String base64Audio = Base64.getEncoder()
            .encodeToString(pcm);

        socket.receiveText(
            "{\"type\":\"audio\",\"audio_event\":{\"audio_base_64\":\"" + base64Audio + "\",\"event_id\":1}}");
    }

    @Override
    protected void providerCallsTool(FakeProviderWebSocket socket, String callId, String name, String argumentsJson) {
        socket.receiveText(
            "{\"type\":\"client_tool_call\",\"client_tool_call\":{\"tool_name\":\"" + name +
                "\",\"tool_call_id\":\"" + callId + "\",\"parameters\":" + argumentsJson + "}}");
    }

    @Override
    protected String expectedToolReplyFragment(String callId, String result) {
        return "\"type\":\"client_tool_result\",\"tool_call_id\":\"" + callId + "\"";
    }

    @Override
    protected void providerFails(FakeProviderWebSocket socket) {
        socket.receiveError(new IllegalStateException("elevenlabs down"));
    }

    @Test
    void testPingIsAnsweredWithPongCarryingTheSameEventId() {
        socket.receiveText("{\"type\":\"ping\",\"ping_event\":{\"event_id\":7}}");

        assertThat(socket.sentTexts()).anySatisfy(
            text -> assertThat(text).contains("\"type\":\"pong\"")
                .contains("\"event_id\":7"));
    }
}
