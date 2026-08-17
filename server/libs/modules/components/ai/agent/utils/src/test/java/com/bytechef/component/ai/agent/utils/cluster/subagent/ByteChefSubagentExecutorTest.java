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

package com.bytechef.component.ai.agent.utils.cluster.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.common.task.subagent.TaskCall;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
class ByteChefSubagentExecutorTest {

    @Test
    void testExecutorReportsByteChefKind() {
        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(null, clusterElement -> List.of());

        assertThat(subagentExecutor.getKind()).isEqualTo(ByteChefSubagentDefinition.KIND);
    }

    @Test
    void testResolvesToolsOnlyFromTheSubagentsOwnElement() {
        AtomicReference<ClusterElement> resolvedClusterElement = new AtomicReference<>();

        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(null, clusterElement -> {
            resolvedClusterElement.set(clusterElement);

            return List.of();
        });

        ClusterElement clusterElement = clusterElement();

        subagentExecutor.resolveToolCallbacks(ByteChefSubagentDefinition.of(clusterElement));

        assertThat(resolvedClusterElement.get()).isSameAs(clusterElement);
    }

    @Test
    void testExecuteFailsWithAnActionableMessageWhenNoModelIsAttached() {
        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(null, clusterElement -> List.of());

        ByteChefSubagentDefinition subagentDefinition = ByteChefSubagentDefinition.of(clusterElement());

        TaskCall taskCall = new TaskCall("Find it", "Find the thing", "Analyst", null, null, Boolean.FALSE);

        assertThatThrownBy(() -> subagentExecutor.execute(taskCall, subagentDefinition))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Analyst")
            .hasMessageContaining("Model");
    }

    @Test
    void testSubagentModelOverridesTheTaskToolModel() {
        ChatModel taskToolChatModel = mock(ChatModel.class);
        ChatModel subagentChatModel = mock(ChatModel.class);

        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(
            taskToolChatModel, clusterElement -> subagentChatModel, clusterElement -> List.of());

        assertThat(subagentExecutor.resolveChatModel(ByteChefSubagentDefinition.of(clusterElement())))
            .isSameAs(subagentChatModel);
    }

    @Test
    void testSubagentWithoutAModelFallsBackToTheTaskToolModel() {
        ChatModel taskToolChatModel = mock(ChatModel.class);

        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(
            taskToolChatModel, clusterElement -> null, clusterElement -> List.of());

        assertThat(subagentExecutor.resolveChatModel(ByteChefSubagentDefinition.of(clusterElement())))
            .isSameAs(taskToolChatModel);
    }

    @Test
    void testSubagentModelIsResolvedFromTheSubagentsOwnElement() {
        AtomicReference<ClusterElement> resolvedClusterElement = new AtomicReference<>();

        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(null, clusterElement -> {
            resolvedClusterElement.set(clusterElement);

            return null;
        }, clusterElement -> List.of());

        ClusterElement clusterElement = clusterElement();

        subagentExecutor.resolveChatModel(ByteChefSubagentDefinition.of(clusterElement));

        assertThat(resolvedClusterElement.get()).isSameAs(clusterElement);
    }

    @Test
    void testExecuteRejectsAForeignSubagentDefinition() {
        ByteChefSubagentExecutor subagentExecutor = new ByteChefSubagentExecutor(null, clusterElement -> List.of());

        TaskCall taskCall = new TaskCall("Find it", "Find the thing", "Analyst", null, null, Boolean.FALSE);

        assertThatThrownBy(() -> subagentExecutor.execute(taskCall, new ForeignSubagentDefinition()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static ClusterElement clusterElement() {
        return new ClusterElement(
            null, null, Map.of(), null, "aiAgentUtils/v1/subagentTool",
            Map.of("subagentName", "Analyst", "description", "Reads", "instructions", "Answer."), "subagent_1");
    }

    private static class ForeignSubagentDefinition
        implements org.springaicommunity.agent.common.task.subagent.SubagentDefinition {

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getKind() {
            return "claude";
        }

        @Override
        public String getName() {
            return "Explore";
        }

        @Override
        public org.springaicommunity.agent.common.task.subagent.SubagentReference getReference() {
            return new org.springaicommunity.agent.common.task.subagent.SubagentReference("explore", "claude");
        }
    }
}
