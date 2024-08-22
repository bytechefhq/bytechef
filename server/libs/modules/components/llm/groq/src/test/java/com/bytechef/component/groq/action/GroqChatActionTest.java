package com.bytechef.component.groq.action;


import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;

import static org.mockito.Mockito.mock;

class GroqChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) GroqChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(OpenAiChatModel.class));
    }
}
