package com.bytechef.component.openai.action;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.llm.action.AbstractLLMActionTest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.Speech;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;

import static com.bytechef.component.llm.constants.LLMConstants.INPUT;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constants.LLMConstants.SPEED;
import static com.bytechef.component.llm.constants.LLMConstants.VOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAICreateSpeechActionTest extends AbstractLLMActionTest {
    private final FileEntry answer = new FileEntry() {
        @Override
        public String getExtension() {
            return ".extension";
        }

        @Override
        public String getMimeType() {
            return "mimeType";
        }

        @Override
        public String getName() {
            return "Name";
        }

        @Override
        public String getUrl() {
            return "http://testUrl";
        }
    };

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(INPUT)).thenReturn("Input");
        when(mockedParameters.get(RESPONSE_FORMAT, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.class))
            .thenReturn(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3);
        when(mockedParameters.getRequiredString(MODEL)).thenReturn("Model");
        when(mockedParameters.get(VOICE, OpenAiAudioApi.SpeechRequest.Voice.class))
            .thenReturn(OpenAiAudioApi.SpeechRequest.Voice.ECHO);
        when(mockedParameters.getFloat(SPEED)).thenReturn(1f);


        try (MockedConstruction<OpenAiAudioSpeechModel> mockedModel = Mockito.mockConstruction(OpenAiAudioSpeechModel.class, (mock, context) -> {
            SpeechResponse mockedSpeechResponse = mock(SpeechResponse.class);
            when(mock.call(any(SpeechPrompt.class))).thenReturn(mockedSpeechResponse);
            when(mockedSpeechResponse.getResult()).thenReturn(new Speech(new byte[0]));
        })) {

            when(mockedContext.file(any())).thenReturn(answer);

            FileEntry response = OpenAICreateSpeechAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(answer, response);
        }
    }
}
