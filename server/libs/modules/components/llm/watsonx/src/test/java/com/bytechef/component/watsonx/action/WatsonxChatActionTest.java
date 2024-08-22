package com.bytechef.component.watsonx.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.watsonx.WatsonxAiChatModel;

import static org.mockito.Mockito.mock;

class WatsonxChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) WatsonxChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(WatsonxAiChatModel.class));
    }
}
