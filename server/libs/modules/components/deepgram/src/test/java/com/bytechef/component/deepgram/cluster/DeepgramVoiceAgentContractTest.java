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

import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.test.voice.AbstractVoiceAgentContractTest;
import com.bytechef.test.voice.FakeProviderWebSocket;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
class DeepgramVoiceAgentContractTest extends AbstractVoiceAgentContractTest {

    @Override
    protected VoiceAgentFunction voiceAgent(ProviderWebSocketConnector connector) {
        return DeepgramVoiceAgent.of(connector)
            .getElement();
    }

    @Override
    protected Map<String, Object> inputParameters() {
        return Map.of("prompt", "Be brief", "greeting", "Hi there");
    }

    @Override
    protected Map<String, Object> connectionParameters() {
        return Map.of("token", "dg-key");
    }

    @Override
    protected String expectedToolRegistrationFragment(String toolName) {
        return "\"functions\":[{\"name\":\"" + toolName + "\"";
    }

    @Override
    protected void providerGreets(FakeProviderWebSocket socket, String text) {
        socket.receiveText("{\"type\":\"ConversationText\",\"role\":\"assistant\",\"content\":\"" + text + "\"}");
    }

    @Override
    protected void providerTranscribes(FakeProviderWebSocket socket, String text) {
        socket.receiveText("{\"type\":\"ConversationText\",\"role\":\"user\",\"content\":\"" + text + "\"}");
    }

    @Override
    protected void providerInterrupts(FakeProviderWebSocket socket) {
        socket.receiveText("{\"type\":\"UserStartedSpeaking\"}");
    }

    @Override
    protected void providerSpeaks(FakeProviderWebSocket socket, byte[] pcm) {
        socket.receiveBinary(pcm);
    }

    @Override
    protected void providerCallsTool(FakeProviderWebSocket socket, String callId, String name, String argumentsJson) {
        socket.receiveText(
            "{\"type\":\"FunctionCallRequest\",\"functions\":[{\"id\":\"" + callId + "\",\"name\":\"" + name +
                "\",\"arguments\":\"" + argumentsJson.replace("\"", "\\\"") + "\",\"client_side\":true}]}");
    }

    @Override
    protected String expectedToolReplyFragment(String callId, String result) {
        return "\"type\":\"FunctionCallResponse\",\"id\":\"" + callId + "\"";
    }

    @Override
    protected void providerFails(FakeProviderWebSocket socket) {
        socket.receiveError(new IllegalStateException("deepgram down"));
    }
}
