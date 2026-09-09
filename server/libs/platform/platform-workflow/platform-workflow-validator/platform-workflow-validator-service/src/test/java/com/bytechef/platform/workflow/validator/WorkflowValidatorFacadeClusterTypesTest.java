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

package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorFacadeClusterTypesTest {

    private static final List<ClusterElementType> AGENT_CLUSTER_ELEMENT_TYPES = List.of(
        new ClusterElementType("MODEL", "model", "Model", true),
        new ClusterElementType("CHAT_MEMORY", "chatMemory", "Memory"),
        new ClusterElementType("RAG", "rag", "RAG"),
        new ClusterElementType("TOOLS", "tools", "Tools", true, false));

    @Test
    void everyClusterElementTypeKeyIsListed() {
        assertEquals(
            List.of("model", "chatMemory", "rag", "tools"),
            WorkflowValidatorFacadeImpl.toClusterElementTypeKeys(AGENT_CLUSTER_ELEMENT_TYPES, false));
    }

    @Test
    void onlyRequiredClusterElementTypeKeysAreListedAsRequired() {
        assertEquals(
            List.of("model"), WorkflowValidatorFacadeImpl.toClusterElementTypeKeys(AGENT_CLUSTER_ELEMENT_TYPES, true));
    }
}
