package com.bytechef.component.openai.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.TranscriptActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;

import java.net.MalformedURLException;

import static org.mockito.Mockito.mock;

class OpenAICreateTranscriptionActionTest extends TranscriptActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) OpenAICreateTranscriptionAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse() throws MalformedURLException {
        getResponseTest(mock(OpenAiAudioTranscriptionModel.class));
    }
}
