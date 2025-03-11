/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.aiagent.ToolFunction.TOOL;
import static com.bytechef.platform.component.definition.aiagent.MemoryFunction.MEMORY;
import static com.bytechef.platform.component.definition.aiagent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.aiagent.RetrievalFunction.RETRIEVAL;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface AiAgentComponentDefinition extends ClusterRootComponentDefinition {

    /**
     *
     */
    String AI_AGENT = "aiAgent";

    @Override
    default List<ClusterElementType> getElementTypes() {
        return List.of(MODEL, MEMORY, RETRIEVAL, TOOL);
    }
}
