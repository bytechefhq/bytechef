package com.bytechef.component.vertex.gemini.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;

import static org.mockito.Mockito.mock;

class VertexGeminiChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) VertexGeminiChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(VertexAiGeminiChatModel.class));
    }
}
