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

package com.bytechef.component.ai.agent.chat.memory.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class AwsChatMemoryV2ComponentHandlerTest {

    @Test
    void testRecursiveSummarizationBuildsTheSummarizerFromTheModelChild() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);
        ModelFunction modelFunction = mock(ModelFunction.class);

        doReturn(mock(ChatModel.class)).when(modelFunction)
            .apply(any(), any(), anyBoolean());

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(eq("openAi"), eq(1), eq("model")))
            .thenReturn(modelFunction);

        ComponentDefinition componentDefinition =
            new AwsChatMemoryV2ComponentHandler(clusterElementDefinitionService).getDefinition();

        List<? extends ClusterElementDefinition<?>> clusterElementDefinitions = componentDefinition.getClusterElements()
            .orElseThrow();

        ChatMemoryFunction chatMemoryFunction = (ChatMemoryFunction) clusterElementDefinitions.getFirst()
            .getElement();

        chatMemoryFunction.apply(
            MockParametersFactory.create(
                Map.of(
                    "conversationId", "conversation-1", "compactionStrategy", "RECURSIVE_SUMMARIZATION",
                    "maxEvents", 20, "maxEventsToKeep", 10, "overlapSize", 2)),
            MockParametersFactory.create(Map.of("accessKeyId", "access-key", "secretAccessKey", "secret-key", "region",
                "us-east-1", "bucket", "memory")),
            MockParametersFactory.create(
                Map.of(
                    "clusterElements",
                    Map.of("model", Map.of("name", "model_1", "type", "openAi/v1/model", "parameters", Map.of())))),
            Map.of("model_1", new ComponentConnection("openAi", 1, 2L, Map.of(), null)));

        verify(modelFunction).apply(any(), any(), eq(false));
    }

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/aws-chat-memory_v2.json",
            new AwsChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class)).getDefinition());
    }

    @Test
    void testIsVersionTwoOfAwsChatMemoryWithAnOptionalSummarizerModel() {
        ComponentDefinition componentDefinition =
            new AwsChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class)).getDefinition();

        assertThat(componentDefinition.getName()).isEqualTo("awsChatMemory");
        assertThat(componentDefinition.getVersion()).isEqualTo(2);
        assertThat(((SessionChatMemoryComponentDefinition) componentDefinition).getClusterElementTypes())
            .containsExactly(SessionChatMemoryComponentDefinition.SUMMARIZER_MODEL);
    }

    @Test
    void testSharesTheConnectionOfVersionOne() {
        ComponentDefinition versionOneComponentDefinition = new AwsChatMemoryComponentHandler().getDefinition();
        ComponentDefinition versionTwoComponentDefinition =
            new AwsChatMemoryV2ComponentHandler(mock(ClusterElementDefinitionService.class)).getDefinition();

        assertThat(versionTwoComponentDefinition.getConnection()
            .orElseThrow()).isSameAs(
                versionOneComponentDefinition.getConnection()
                    .orElseThrow());
    }
}
