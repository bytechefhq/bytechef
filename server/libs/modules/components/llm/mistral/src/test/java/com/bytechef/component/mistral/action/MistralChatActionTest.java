package com.bytechef.component.mistral.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mistralai.MistralAiChatModel;

import static org.mockito.Mockito.mock;

class MistralChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) MistralChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(MistralAiChatModel.class));
    }
}
