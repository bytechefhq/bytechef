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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ElevenLabsVoiceAgentTest {

    @Test
    void testOutputFormatMismatchIsReported() {
        String warning = ElevenLabsVoiceAgent.outputFormatMismatchWarning(metadata("pcm_24000"), 16000);

        assertThat(warning).contains("pcm_24000")
            .contains("16000");
    }

    @Test
    void testMatchingOutputFormatIsNotReported() {
        assertThat(ElevenLabsVoiceAgent.outputFormatMismatchWarning(metadata("pcm_16000"), 16000)).isNull();
    }

    @Test
    void testMissingOrNonPcmOutputFormatIsNotReported() {
        assertThat(
            ElevenLabsVoiceAgent.outputFormatMismatchWarning(
                Map.of("type", "conversation_initiation_metadata"), 16000)).isNull();
        assertThat(ElevenLabsVoiceAgent.outputFormatMismatchWarning(metadata("ulaw_8000"), 16000)).isNull();
    }

    private static Map<String, Object> metadata(String agentOutputAudioFormat) {
        return Map.of(
            "type", "conversation_initiation_metadata", "conversation_initiation_metadata_event",
            Map.of("agent_output_audio_format", agentOutputAudioFormat, "user_input_audio_format", "pcm_16000"));
    }

    @Test
    void testToVoiceEventMapsAgentResponseToAssistantText() {
        Map<String, Object> voiceEvent = ElevenLabsVoiceAgent.toVoiceEvent(
            Map.of("type", "agent_response", "agent_response_event", Map.of("agent_response", "Sure, where to?")));

        assertThat(voiceEvent).containsEntry("type", "assistant_text")
            .containsEntry("text", "Sure, where to?");
    }

    @Test
    void testToVoiceEventMapsUserTranscriptToTranscriptFinal() {
        Map<String, Object> voiceEvent = ElevenLabsVoiceAgent.toVoiceEvent(
            Map.of("type", "user_transcript", "user_transcription_event", Map.of("user_transcript", "book a flight")));

        assertThat(voiceEvent).containsEntry("type", "transcript_final")
            .containsEntry("text", "book a flight");
    }

    @Test
    void testToVoiceEventMapsInterruptionToSpeechStart() {
        Map<String, Object> voiceEvent = ElevenLabsVoiceAgent.toVoiceEvent(
            Map.of("type", "interruption", "interruption_event", Map.of("event_id", 1)));

        assertThat(voiceEvent).containsEntry("type", "speech_start");
    }

    @Test
    void testToVoiceEventTreatsMissingAgentResponseTextAsEmptyText() {
        Map<String, Object> elevenLabsMessage = new HashMap<>();

        elevenLabsMessage.put("type", "agent_response");
        elevenLabsMessage.put("agent_response_event", Map.of());

        Map<String, Object> voiceEvent = ElevenLabsVoiceAgent.toVoiceEvent(elevenLabsMessage);

        assertThat(voiceEvent).containsEntry("type", "assistant_text")
            .containsEntry("text", "");
    }

    @Test
    void testToVoiceEventTreatsMissingEventMapAsEmptyText() {
        Map<String, Object> voiceEvent = ElevenLabsVoiceAgent.toVoiceEvent(Map.of("type", "user_transcript"));

        assertThat(voiceEvent).containsEntry("type", "transcript_final")
            .containsEntry("text", "");
    }

    @Test
    void testToVoiceEventReturnsNullForControlFrames() {
        assertThat(ElevenLabsVoiceAgent.toVoiceEvent(Map.of("type", "conversation_initiation_metadata"))).isNull();
        assertThat(ElevenLabsVoiceAgent.toVoiceEvent(Map.of("type", "ping"))).isNull();
        assertThat(ElevenLabsVoiceAgent.toVoiceEvent(Map.of("type", "client_tool_call"))).isNull();
        assertThat(ElevenLabsVoiceAgent.toVoiceEvent(Map.of("type", "audio"))).isNull();
    }

    @Test
    void testToVoiceEventReturnsNullWhenTypeMissing() {
        assertThat(ElevenLabsVoiceAgent.toVoiceEvent(Map.of("role", "user"))).isNull();
    }

    @Test
    void testAgentToolNamesResolveClientToolIdsThroughTheToolsEndpoint() {
        Map<String, Object> agent = agentWithPrompt(Map.of("tool_ids", List.of("tool_1", "tool_2", "tool_3")));
        Map<String, Map<String, Object>> tools = Map.of(
            "tool_1", Map.of("id", "tool_1", "tool_config", Map.of("type", "client", "name", "lookupOrder")),
            "tool_2", Map.of("id", "tool_2", "tool_config", Map.of("type", "webhook", "name", "notifySlack")));

        Set<String> toolNames = ElevenLabsVoiceAgent.resolveAgentToolNames(agent, toolId -> {
            if (!tools.containsKey(toolId)) {
                throw new IllegalStateException("No tool " + toolId);
            }

            return tools.get(toolId);
        });

        assertThat(toolNames).containsExactly("lookupOrder");
    }

    @Test
    void testAgentToolNamesStillReadTheDeprecatedInlineToolsArray() {
        Map<String, Object> agent = agentWithPrompt(
            Map.of("tools", List.of(Map.of("type", "client", "name", "lookupOrder"))));

        Set<String> toolNames = ElevenLabsVoiceAgent.resolveAgentToolNames(agent, toolId -> {
            throw new AssertionError("No tool lookup expected for " + toolId);
        });

        assertThat(toolNames).containsExactly("lookupOrder");
    }

    @Test
    void testAgentToolNamesAreEmptyWithoutAPrompt() {
        assertThat(ElevenLabsVoiceAgent.resolveAgentToolNames(Map.of("agent_id", "agent_1"), toolId -> Map.of()))
            .isEmpty();
    }

    private static Map<String, Object> agentWithPrompt(Map<String, Object> prompt) {
        return Map.of("conversation_config", Map.of("agent", Map.of("prompt", prompt)));
    }
}
