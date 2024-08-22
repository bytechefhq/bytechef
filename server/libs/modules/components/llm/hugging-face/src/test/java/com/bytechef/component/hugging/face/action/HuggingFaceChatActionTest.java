package com.bytechef.component.hugging.face.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.huggingface.HuggingfaceChatModel;

import static org.mockito.Mockito.mock;

class HuggingFaceChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) HuggingFaceChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(HuggingfaceChatModel.class));
    }
}
