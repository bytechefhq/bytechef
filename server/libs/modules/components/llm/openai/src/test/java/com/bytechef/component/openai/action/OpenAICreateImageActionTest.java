package com.bytechef.component.openai.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ImageActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiImageModel;

import static org.mockito.Mockito.mock;

class OpenAICreateImageActionTest extends ImageActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) OpenAICreateImageAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(OpenAiImageModel.class));
    }
}
