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

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;

/**
 * @author Ivica Cardic
 */
class ByteChefSubagentResolverTest {

    @Test
    void testResolvesAttachedSubagentByWorkflowNodeName() {
        ClusterElement clusterElement = clusterElement("subagent_1", "Analyst", "Answers questions");

        ByteChefSubagentResolver resolver = new ByteChefSubagentResolver(List.of(clusterElement));

        SubagentReference subagentReference = new SubagentReference("subagent_1", ByteChefSubagentDefinition.KIND);

        assertThat(resolver.canResolve(subagentReference)).isTrue();

        SubagentDefinition subagentDefinition = resolver.resolve(subagentReference);

        assertThat(subagentDefinition.getName()).isEqualTo("Analyst");
        assertThat(subagentDefinition.getDescription()).isEqualTo("Answers questions");
        assertThat(subagentDefinition.getKind()).isEqualTo(ByteChefSubagentDefinition.KIND);
    }

    @Test
    void testCannotResolveUnknownUriOrForeignKind() {
        ByteChefSubagentResolver resolver = new ByteChefSubagentResolver(
            List.of(clusterElement("subagent_1", "Analyst", "Answers questions")));

        assertThat(resolver.canResolve(new SubagentReference("subagent_2", ByteChefSubagentDefinition.KIND)))
            .isFalse();
        assertThat(resolver.canResolve(new SubagentReference("subagent_1", "claude")))
            .isFalse();

        assertThatThrownBy(() -> resolver.resolve(new SubagentReference("subagent_2", ByteChefSubagentDefinition.KIND)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testExposesOneReferencePerAttachedSubagent() {
        ByteChefSubagentResolver resolver = new ByteChefSubagentResolver(
            List.of(
                clusterElement("subagent_1", "Analyst", "Reads"),
                clusterElement("subagent_2", "Curator", "Writes")));

        assertThat(resolver.getReferences())
            .extracting(SubagentReference::uri)
            .containsExactly("subagent_1", "subagent_2");
    }

    private static ClusterElement clusterElement(String workflowNodeName, String name, String description) {
        return new ClusterElement(
            null, null, Map.of(), null, "aiAgentUtils/v1/subagentTool",
            Map.of("subagentName", name, "description", description, "instructions", "Do the thing."),
            workflowNodeName);
    }
}
