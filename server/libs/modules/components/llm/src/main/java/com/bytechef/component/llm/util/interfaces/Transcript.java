package com.bytechef.component.llm.util.interfaces;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;

import static com.bytechef.component.llm.constants.LLMConstants.FILE;

public interface Transcript {
    private static AudioTranscriptionPrompt getTranscriptionPrompt(Parameters inputParameters) throws MalformedURLException {
        FileEntry fileEntry = inputParameters.getFileEntry(FILE);
        return new AudioTranscriptionPrompt(new UrlResource(fileEntry.getUrl()));
    }

    static String getResponse(Transcript transcript, Parameters inputParameters, Parameters connectionParameters) throws MalformedURLException {
        Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> transcriptionModel = transcript.createTranscriptionModel(inputParameters, connectionParameters);

        AudioTranscriptionPrompt transcriptionPrompt = Transcript.getTranscriptionPrompt(inputParameters);

        AudioTranscriptionResponse response = transcriptionModel.call(transcriptionPrompt);
        return response.getResult()
            .getOutput();
    }
    AudioTranscriptionOptions createTranscriptOptions(Parameters inputParameters);
    Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> createTranscriptionModel(Parameters inputParameters, Parameters connectionParameters);
}
