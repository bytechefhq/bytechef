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

import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;
import org.springaicommunity.agent.common.task.subagent.SubagentResolver;

/**
 * Resolves references minted from the SUBAGENT cluster elements attached to one task tool instance.
 *
 * <p>
 * Fails closed: a reference naming a subagent that is not attached, or one belonging to another implementation's kind,
 * is not resolvable. Only what the builder wired onto this task tool can ever be resolved.
 *
 * @author Ivica Cardic
 */
public class ByteChefSubagentResolver implements SubagentResolver {

    private final Map<String, ByteChefSubagentDefinition> subagentDefinitions;

    public ByteChefSubagentResolver(List<ClusterElement> clusterElements) {
        Map<String, ByteChefSubagentDefinition> definitions = new LinkedHashMap<>();

        for (ClusterElement clusterElement : clusterElements) {
            definitions.put(clusterElement.getWorkflowNodeName(), ByteChefSubagentDefinition.of(clusterElement));
        }

        this.subagentDefinitions = definitions;
    }

    @Override
    public boolean canResolve(SubagentReference subagentReference) {
        return Objects.equals(subagentReference.kind(), ByteChefSubagentDefinition.KIND) &&
            subagentDefinitions.containsKey(subagentReference.uri());
    }

    public List<SubagentReference> getReferences() {
        return subagentDefinitions.values()
            .stream()
            .map(SubagentDefinition::getReference)
            .toList();
    }

    @Override
    public SubagentDefinition resolve(SubagentReference subagentReference) {
        ByteChefSubagentDefinition subagentDefinition = subagentDefinitions.get(subagentReference.uri());

        if (subagentDefinition == null) {
            throw new IllegalArgumentException(
                "No subagent attached for reference '%s'".formatted(subagentReference.uri()));
        }

        return subagentDefinition;
    }
}
