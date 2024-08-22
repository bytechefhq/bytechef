package com.bytechef.component.qianfan.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ImageActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.qianfan.QianFanImageModel;

import static org.mockito.Mockito.mock;

class QIanFanCreateImageActionTest extends ImageActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) QIanFanCreateImageAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(QianFanImageModel.class));
    }
}
