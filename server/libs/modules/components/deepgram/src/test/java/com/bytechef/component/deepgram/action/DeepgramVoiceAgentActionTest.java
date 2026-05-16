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

package com.bytechef.component.deepgram.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DeepgramVoiceAgentActionTest {

    @Test
    void testToVoiceEventMapsUserConversationTextToFinalTranscript() {
        Map<String, Object> voiceEvent = DeepgramVoiceAgentAction.toVoiceEvent(
            Map.of("type", "ConversationText", "role", "user", "content", "book a flight"));

        assertThat(voiceEvent).containsEntry("type", "transcript_final")
            .containsEntry("text", "book a flight");
    }

    @Test
    void testToVoiceEventMapsAssistantConversationTextToAssistantText() {
        Map<String, Object> voiceEvent = DeepgramVoiceAgentAction.toVoiceEvent(
            Map.of("type", "ConversationText", "role", "assistant", "content", "Sure, where to?"));

        assertThat(voiceEvent).containsEntry("type", "assistant_text")
            .containsEntry("text", "Sure, where to?");
    }

    @Test
    void testToVoiceEventMapsUserStartedSpeakingToSpeechStart() {
        Map<String, Object> voiceEvent = DeepgramVoiceAgentAction.toVoiceEvent(Map.of("type", "UserStartedSpeaking"));

        assertThat(voiceEvent).containsEntry("type", "speech_start");
    }

    @Test
    void testToVoiceEventTreatsMissingRoleAsTranscript() {
        Map<String, Object> voiceEvent = DeepgramVoiceAgentAction.toVoiceEvent(
            Map.of("type", "ConversationText", "content", "hello"));

        assertThat(voiceEvent).containsEntry("type", "transcript_final")
            .containsEntry("text", "hello");
    }

    @Test
    void testToVoiceEventTreatsMissingContentAsEmptyText() {
        Map<String, Object> deepgramMessage = new HashMap<>();

        deepgramMessage.put("type", "ConversationText");
        deepgramMessage.put("role", "assistant");

        Map<String, Object> voiceEvent = DeepgramVoiceAgentAction.toVoiceEvent(deepgramMessage);

        assertThat(voiceEvent).containsEntry("type", "assistant_text")
            .containsEntry("text", "");
    }

    @Test
    void testToVoiceEventReturnsNullForControlFrames() {
        assertThat(DeepgramVoiceAgentAction.toVoiceEvent(Map.of("type", "Welcome"))).isNull();
        assertThat(DeepgramVoiceAgentAction.toVoiceEvent(Map.of("type", "SettingsApplied"))).isNull();
    }

    @Test
    void testToVoiceEventReturnsNullWhenTypeMissing() {
        assertThat(DeepgramVoiceAgentAction.toVoiceEvent(Map.of("role", "user"))).isNull();
    }
}
