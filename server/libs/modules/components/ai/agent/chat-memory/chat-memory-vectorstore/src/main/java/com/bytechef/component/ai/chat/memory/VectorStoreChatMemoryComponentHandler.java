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

package com.bytechef.component.ai.chat.memory;

import static com.bytechef.component.ai.chat.memory.VectorStoreChatMemoryComponentHandler.VECTOR_STORE_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.chat.memory.cluster.VectorStoreChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(VECTOR_STORE_CHAT_MEMORY + "_v1_ComponentHandler")
public class VectorStoreChatMemoryComponentHandler implements ComponentHandler {

    public static final String VECTOR_STORE_CHAT_MEMORY = "vectorStoreChatMemory";

    private final VectorStoreChatMemoryComponentDefinition componentDefinition;

    public VectorStoreChatMemoryComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new VectorStoreChatMemoryComponentDefinitionImpl(
            component(VECTOR_STORE_CHAT_MEMORY)
                .title("Vector Store Chat Memory")
                .description("Vector Store Chat Memory.")
                .icon("path:assets/vector-store-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new VectorStoreChatMemory(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class VectorStoreChatMemoryComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreChatMemoryComponentDefinition {

        public VectorStoreChatMemoryComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
