package com.bytechef.component.stability.action;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.action.ImageActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.stabilityai.StabilityAiImageModel;

import static org.mockito.Mockito.mock;

class StabilityCreateImageActionTest extends ImageActionTest {
    @Test
    void testPerform() {
        performTest((ActionDefinition.SingleConnectionPerformFunction) StabilityCreateImageAction.ACTION_DEFINITION.getPerform().get());
    }

    @Test
    void testGetResponse(){
        getResponseTest(mock(StabilityAiImageModel.class));
    }
}
