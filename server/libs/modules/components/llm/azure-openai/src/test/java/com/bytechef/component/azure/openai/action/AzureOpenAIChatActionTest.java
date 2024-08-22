package com.bytechef.component.azure.openai.action;


import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;

import static org.mockito.Mockito.mock;

class AzureOpenAIChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) AzureOpenAIChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(AzureOpenAiChatModel.class));
    }
}
