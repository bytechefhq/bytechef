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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springaicommunity.agent.common.task.subagent.SubagentDefinition;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;

/**
 * A subagent definition backed by a single SUBAGENT cluster element. The persona — name, description and instructions —
 * comes from the element's own parameters, so it is authored by the workflow builder rather than shipped with the
 * product.
 *
 * <p>
 * The element's workflow node name is the reference uri, which is what lets {@link ByteChefSubagentResolver} map a
 * reference handed back by the task tool to the element that produced it.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ByteChefSubagentDefinition implements SubagentDefinition {

    public static final String KIND = "bytechef";

    private static final String DESCRIPTION = "description";
    private static final String INSTRUCTIONS = "instructions";
    private static final String SUBAGENT_NAME = "subagentName";

    private final ClusterElement clusterElement;
    private final String description;
    private final String name;

    private ByteChefSubagentDefinition(ClusterElement clusterElement, String name, String description) {
        this.clusterElement = clusterElement;
        this.description = description;
        this.name = name;
    }

    public static ByteChefSubagentDefinition of(ClusterElement clusterElement) {
        Map<String, ?> parameters = clusterElement.getParameters();

        Object name = parameters.get(SUBAGENT_NAME);
        Object description = parameters.get(DESCRIPTION);

        return new ByteChefSubagentDefinition(
            clusterElement, name == null ? clusterElement.getWorkflowNodeName() : name.toString(),
            description == null ? "" : description.toString());
    }

    public ClusterElement getClusterElement() {
        return clusterElement;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getInstructions() {
        Map<String, ?> parameters = clusterElement.getParameters();

        Object instructions = parameters.get(INSTRUCTIONS);

        return instructions == null ? "" : instructions.toString();
    }

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SubagentReference getReference() {
        return new SubagentReference(clusterElement.getWorkflowNodeName(), KIND);
    }
}
