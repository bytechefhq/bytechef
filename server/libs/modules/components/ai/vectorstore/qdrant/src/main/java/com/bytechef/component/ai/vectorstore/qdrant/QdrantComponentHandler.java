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

package com.bytechef.component.ai.vectorstore.qdrant;

import static com.bytechef.component.ai.vectorstore.qdrant.constant.QdrantConstants.QDRANT;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.qdrant.action.QdrantLoadAction;
import com.bytechef.component.ai.vectorstore.qdrant.action.QdrantSearchAction;
import com.bytechef.component.ai.vectorstore.qdrant.cluster.QdrantVectorStore;
import com.bytechef.component.ai.vectorstore.qdrant.connection.QdrantConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(QDRANT + "_v1_ComponentHandler")
public class QdrantComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public QdrantComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new QdrantComponentDefinitionImpl(
            component(QDRANT)
                .title("Qdrant")
                .description(
                    "Qdrant is an open-source vector similarity search engine designed to handle high-dimensional " +
                        "data, enabling efficient and scalable nearest neighbor search for applications like " +
                        "recommendation systems and machine learning.")
                .icon("path:assets/qdrant.svg")
                .connection(QdrantConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    QdrantLoadAction.of(clusterElementDefinitionService),
                    QdrantSearchAction.of(clusterElementDefinitionService))
                .clusterElements(
                    QdrantVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class QdrantComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public QdrantComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
