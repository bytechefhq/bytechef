package com.bytechef.component.zhipu.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ImageActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.zhipuai.ZhiPuAiImageModel;

import static org.mockito.Mockito.mock;

class ZhiPuCreateImageActionTest extends ImageActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) ZhiPuCreateImageAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(ZhiPuAiImageModel.class));
    }
}
