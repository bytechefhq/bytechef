package com.bytechef.component.llm.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.llm.util.interfaces.Chat;
import com.bytechef.component.llm.util.interfaces.Transcript;
import com.bytechef.component.llm.util.records.MessageRecord;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Model;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;

import java.net.MalformedURLException;
import java.util.List;

import static com.bytechef.component.llm.constants.LLMConstants.FILE;
import static com.bytechef.component.llm.constants.LLMConstants.MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class TranscriptActionTest extends AbstractLLMActionTest{
    private final String answer = "ANSWER";

    protected void performTest(ActionDefinition.SingleConnectionPerformFunction perform){
        try (MockedStatic<Transcript> mockedTranscript = Mockito.mockStatic(Transcript.class)) {
            mockedTranscript.when(() -> Transcript.getResponse(any(Transcript.class), eq(mockedParameters), eq(mockedParameters)))
                .thenReturn(answer);

            String result = (String) perform.apply(mockedParameters, mockedParameters, mockedContext);

            assertEquals(answer, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void getResponseTest(Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> mockedTranscriptModel) throws MalformedURLException {
        when(mockedParameters.getFileEntry(FILE))
            .thenReturn(new FileEntry() {
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
            });

        Transcript mockedTranscription = mock(Transcript.class);
        AudioTranscriptionResponse transcriptionResponse = new AudioTranscriptionResponse(new AudioTranscription(answer));
        AudioTranscriptionResponse mockedTranscriptionResponse = spy(transcriptionResponse);

        when(mockedTranscription.createTranscriptionModel(mockedParameters, mockedParameters)).thenReturn(mockedTranscriptModel);
        when(mockedTranscriptModel.call(any(AudioTranscriptionPrompt.class))).thenReturn(mockedTranscriptionResponse);

        String response = Transcript.getResponse(mockedTranscription, mockedParameters, mockedParameters);

        assertEquals(answer, response);
    }
}
