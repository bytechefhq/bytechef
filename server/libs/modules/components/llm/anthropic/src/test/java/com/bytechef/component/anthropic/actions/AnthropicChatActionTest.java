package com.bytechef.component.anthropic.actions;

import com.bytechef.component.anthropic.action.AnthropicChatAction;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;

import static org.mockito.Mockito.mock;

class AnthropicChatActionTest extends ChatActionTest {

    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) AnthropicChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(AnthropicChatModel.class));
    }
}
