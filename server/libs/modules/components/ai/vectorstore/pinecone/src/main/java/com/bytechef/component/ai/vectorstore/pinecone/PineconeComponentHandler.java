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

package com.bytechef.component.ai.vectorstore.pinecone;

import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.PINECONE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.pinecone.action.PineconeLoadAction;
import com.bytechef.component.ai.vectorstore.pinecone.action.PineconeSearchAction;
import com.bytechef.component.ai.vectorstore.pinecone.cluster.PineconeVectorStore;
import com.bytechef.component.ai.vectorstore.pinecone.connection.PineconeConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(PINECONE + "_v1_ComponentHandler")
public class PineconeComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public PineconeComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new PineconeComponentDefinitionImpl(
            component(PINECONE)
                .title("Pinecone")
                .description(
                    "Pinecone is a vector database designed for efficient similarity search and storage of " +
                        "high-dimensional data, commonly used in machine learning and AI applications.")
                .icon("path:assets/pinecone.svg")
                .connection(PineconeConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    new PineconeLoadAction(clusterElementDefinitionService).actionDefinition,
                    new PineconeSearchAction(clusterElementDefinitionService).actionDefinition)
                .clusterElements(
                    new PineconeVectorStore(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class PineconeComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public PineconeComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
