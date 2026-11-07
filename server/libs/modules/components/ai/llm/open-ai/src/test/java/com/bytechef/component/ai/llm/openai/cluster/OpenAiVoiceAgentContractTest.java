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

import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.test.voice.AbstractVoiceAgentContractTest;
import com.bytechef.test.voice.FakeProviderWebSocket;
import java.util.Base64;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
class OpenAiVoiceAgentContractTest extends AbstractVoiceAgentContractTest {

    @Override
    protected VoiceAgentFunction voiceAgent(ProviderWebSocketConnector connector) {
        return OpenAiVoiceAgent.of(connector)
            .getElement();
    }

    @Override
    protected Map<String, Object> inputParameters() {
        return Map.of("instructions", "Be brief", "greeting", "Hi there");
    }

    @Override
    protected Map<String, Object> connectionParameters() {
        return Map.of("token", "sk-test");
    }

    @Override
    protected String expectedToolRegistrationFragment(String toolName) {
        return "\"tools\":[{\"type\":\"function\",\"name\":\"" + toolName + "\"";
    }

    @Override
    protected void providerGreets(FakeProviderWebSocket socket, String text) {
        socket.receiveText("{\"type\":\"response.output_audio_transcript.done\",\"transcript\":\"" + text + "\"}");
    }

    @Override
    protected void providerTranscribes(FakeProviderWebSocket socket, String text) {
        socket.receiveText(
            "{\"type\":\"conversation.item.input_audio_transcription.completed\",\"transcript\":\"" + text + "\"}");
    }

    @Override
    protected void providerInterrupts(FakeProviderWebSocket socket) {
        socket.receiveText("{\"type\":\"input_audio_buffer.speech_started\"}");
    }

    @Override
    protected void providerSpeaks(FakeProviderWebSocket socket, byte[] pcm) {
        String base64Audio = Base64.getEncoder()
            .encodeToString(pcm);

        socket.receiveText("{\"type\":\"response.output_audio.delta\",\"delta\":\"" + base64Audio + "\"}");
    }

    @Override
    protected void providerCallsTool(FakeProviderWebSocket socket, String callId, String name, String argumentsJson) {
        socket.receiveText(
            "{\"type\":\"response.function_call_arguments.done\",\"call_id\":\"" + callId + "\",\"name\":\"" + name +
                "\",\"arguments\":\"" + argumentsJson.replace("\"", "\\\"") + "\"}");
    }

    @Override
    protected String expectedToolReplyFragment(String callId, String result) {
        return "\"type\":\"function_call_output\",\"call_id\":\"" + callId + "\"";
    }

    @Override
    protected void providerFails(FakeProviderWebSocket socket) {
        socket.receiveText("{\"type\":\"error\",\"error\":{\"message\":\"boom\"}}");
    }
}
