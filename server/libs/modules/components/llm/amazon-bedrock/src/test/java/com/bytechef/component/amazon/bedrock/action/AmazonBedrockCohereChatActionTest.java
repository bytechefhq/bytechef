package com.bytechef.component.amazon.bedrock.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatModel;

import static org.mockito.Mockito.mock;

class AmazonBedrockCohereChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) AmazonBedrockCohereChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(BedrockAnthropicChatModel.class));
    }
}
