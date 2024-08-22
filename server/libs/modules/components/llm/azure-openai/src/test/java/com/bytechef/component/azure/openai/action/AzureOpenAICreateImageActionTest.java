package com.bytechef.component.azure.openai.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ImageActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.azure.openai.AzureOpenAiImageModel;

import static org.mockito.Mockito.mock;

class AzureOpenAICreateImageActionTest extends ImageActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) AzureOpenAICreateImageAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(AzureOpenAiImageModel.class));
    }
}
