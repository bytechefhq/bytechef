package com.bytechef.component.vertex.palm2.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vertexai.palm2.VertexAiPaLm2ChatModel;

import static org.mockito.Mockito.mock;

class VertexPaLM2ChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) VertexPaLM2ChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(VertexAiPaLm2ChatModel.class));
    }
}
