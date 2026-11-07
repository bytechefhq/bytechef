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

import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property.StringProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class OpenAiVoiceAgentTest {

    @Test
    void testToVoiceEventMapsSpeechStartedToSpeechStart() {
        Map<String, Object> voiceEvent = OpenAiVoiceAgent.toVoiceEvent(
            Map.of("type", "input_audio_buffer.speech_started"));

        assertThat(voiceEvent).containsEntry("type", "speech_start");
    }

    @Test
    void testToVoiceEventMapsInputAudioTranscriptionCompletedToTranscriptFinal() {
        Map<String, Object> voiceEvent = OpenAiVoiceAgent.toVoiceEvent(
            Map.of("type", "conversation.item.input_audio_transcription.completed", "transcript", "book a flight"));

        assertThat(voiceEvent).containsEntry("type", "transcript_final")
            .containsEntry("text", "book a flight");
    }

    @Test
    void testToVoiceEventMapsOutputAudioTranscriptDoneToAssistantText() {
        Map<String, Object> voiceEvent = OpenAiVoiceAgent.toVoiceEvent(
            Map.of("type", "response.output_audio_transcript.done", "transcript", "Sure, where to?"));

        assertThat(voiceEvent).containsEntry("type", "assistant_text")
            .containsEntry("text", "Sure, where to?");
    }

    @Test
    void testToVoiceEventTreatsMissingTranscriptAsEmptyText() {
        Map<String, Object> openAiMessage = new HashMap<>();

        openAiMessage.put("type", "response.output_audio_transcript.done");

        Map<String, Object> voiceEvent = OpenAiVoiceAgent.toVoiceEvent(openAiMessage);

        assertThat(voiceEvent).containsEntry("type", "assistant_text")
            .containsEntry("text", "");
    }

    @Test
    void testToVoiceEventReturnsNullForResponseDone() {
        assertThat(OpenAiVoiceAgent.toVoiceEvent(Map.of("type", "response.done"))).isNull();
    }

    @Test
    void testToVoiceEventReturnsNullForFunctionCallArgumentsDone() {
        assertThat(
            OpenAiVoiceAgent.toVoiceEvent(Map.of("type", "response.function_call_arguments.done"))).isNull();
    }

    @Test
    void testToVoiceEventReturnsNullForAudioDelta() {
        assertThat(OpenAiVoiceAgent.toVoiceEvent(Map.of("type", "response.output_audio.delta"))).isNull();
    }

    @Test
    void testToVoiceEventReturnsNullWhenTypeMissing() {
        assertThat(OpenAiVoiceAgent.toVoiceEvent(Map.of("transcript", "hello"))).isNull();
    }

    @Test
    void testModelOptionsOfferTheGaRealtimeModels() {
        StringProperty modelProperty = OpenAiVoiceAgent.CLUSTER_ELEMENT_DEFINITION.getProperties()
            .stream()
            .filter(property -> "model".equals(property.getName()))
            .map(StringProperty.class::cast)
            .findFirst()
            .orElseThrow();

        List<String> optionValues = modelProperty.getOptions()
            .stream()
            .map(Option::getValue)
            .toList();

        assertThat(optionValues).containsExactly(
            "gpt-realtime", "gpt-realtime-1.5", "gpt-realtime-2", "gpt-realtime-2.1", "gpt-realtime-mini",
            "gpt-realtime-2.1-mini", "gpt-4o-realtime-preview", "gpt-4o-mini-realtime-preview");
        assertThat(modelProperty.getDefaultValue()).contains("gpt-realtime");
    }
}
