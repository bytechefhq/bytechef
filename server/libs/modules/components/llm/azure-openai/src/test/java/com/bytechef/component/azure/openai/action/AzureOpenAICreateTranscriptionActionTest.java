package com.bytechef.component.azure.openai.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.TranscriptActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionModel;

import java.net.MalformedURLException;

import static org.mockito.Mockito.mock;

class AzureOpenAICreateTranscriptionActionTest extends TranscriptActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) AzureOpenAICreateTranscriptionAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse() throws MalformedURLException {
        getResponseTest(mock(AzureOpenAiAudioTranscriptionModel.class));
    }
}
