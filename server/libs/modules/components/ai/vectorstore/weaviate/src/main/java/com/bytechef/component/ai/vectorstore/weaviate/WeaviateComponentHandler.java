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

package com.bytechef.component.ai.vectorstore.weaviate;

import static com.bytechef.component.ai.vectorstore.weaviate.constant.WeaviateConstants.WEAVIATE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.weaviate.action.WeaviateLoadAction;
import com.bytechef.component.ai.vectorstore.weaviate.action.WeaviateSearchAction;
import com.bytechef.component.ai.vectorstore.weaviate.cluster.WeaviateVectorStore;
import com.bytechef.component.ai.vectorstore.weaviate.connection.WeaviateConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(WEAVIATE + "_v1_ComponentHandler")
public class WeaviateComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public WeaviateComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new WeaviateComponentDefinitionImpl(component(WEAVIATE)
            .title("Weaviate")
            .description(
                "Weaviate is an open-source vector search engine and database that enables efficient storage, " +
                    "retrieval, and management of high-dimensional data, often used in machine learning and AI " +
                    "applications.")
            .icon("path:assets/weaviate.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .connection(WeaviateConnection.CONNECTION_DEFINITION)
            .actions(
                WeaviateSearchAction.of(clusterElementDefinitionService),
                WeaviateLoadAction.of(clusterElementDefinitionService))
            .clusterElements(WeaviateVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class WeaviateComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public WeaviateComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
