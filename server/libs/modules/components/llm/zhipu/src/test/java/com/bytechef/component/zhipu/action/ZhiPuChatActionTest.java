package com.bytechef.component.zhipu.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ChatActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;

import static org.mockito.Mockito.mock;

class ZhiPuChatActionTest extends ChatActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) ZhiPuChatAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(ZhiPuAiChatModel.class));
    }
}
