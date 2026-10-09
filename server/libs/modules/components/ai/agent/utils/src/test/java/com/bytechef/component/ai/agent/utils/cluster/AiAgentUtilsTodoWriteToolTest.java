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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsTodoWriteToolTest {

    @Test
    void testDefinitionIsAToolTheAiAgentAccepts() {
        assertThat(AiAgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION.getType()).isEqualTo(TOOLS);
    }

    @Test
    void testApplyProvidesTheTodoWriteToolCallback() throws Exception {
        ToolCallbackProvider toolCallbackProvider = AiAgentUtilsTodoWriteTool.CLUSTER_ELEMENT_DEFINITION
            .getElement()
            .apply(mock(Parameters.class), mock(Parameters.class), mock(Context.class));

        assertThat(Arrays.stream(toolCallbackProvider.getToolCallbacks())
            .map(ToolCallback::getToolDefinition)
            .map(ToolDefinition::name)).containsExactly("TodoWrite");
    }
}
