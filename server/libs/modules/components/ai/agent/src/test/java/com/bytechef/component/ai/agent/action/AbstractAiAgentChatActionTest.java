/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agent.action;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AbstractAiAgentChatActionTest {

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Test
    void testGetChatClientRequestSpecWithNullParameterValues() throws Exception {
        HashMap<String, Object> inputParamsMap = new HashMap<>();

        inputParamsMap.put("key1", "value1");
        inputParamsMap.put("nullableKey", null);

        Parameters inputParameters = MockParametersFactory.create(inputParamsMap);

        HashMap<String, Object> clusterElementParams = new HashMap<>();

        clusterElementParams.put("model", "gpt-4o");
        clusterElementParams.put("nullableParam", null);

        Map<String, Object> modelElement = new HashMap<>();

        modelElement.put("name", "model_1");
        modelElement.put("type", "testComponent/v1/testModel");
        modelElement.put("parameters", clusterElementParams);

        Parameters extensions = MockParametersFactory.create(
            Map.of("clusterElements", Map.of("model", modelElement)));

        ModelFunction modelFunction = mock(ModelFunction.class);

        ChatModel chatModel = mock(ChatModel.class);

        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);
        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);

        Map<String, ComponentConnection> connectionParameters = Map.of("model_1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(clusterElementDefinitionService);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertDoesNotThrow(
                () -> action.getChatClientRequestSpec(inputParameters, connectionParameters, extensions,
                    actionContext));
        }
    }

    private static class TestAiAgentChatAction extends AbstractAiAgentChatAction {

        TestAiAgentChatAction(ClusterElementDefinitionService clusterElementDefinitionService) {
            super(clusterElementDefinitionService);
        }
    }
}
