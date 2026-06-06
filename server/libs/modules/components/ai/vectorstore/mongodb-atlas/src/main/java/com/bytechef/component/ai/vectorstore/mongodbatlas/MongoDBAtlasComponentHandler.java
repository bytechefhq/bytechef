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

package com.bytechef.component.ai.vectorstore.mongodbatlas;

import static com.bytechef.component.ai.vectorstore.mongodbatlas.constant.MongoDBAtlasConstants.MONGODB_ATLAS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.mongodbatlas.action.MongoDBAtlasDeleteAction;
import com.bytechef.component.ai.vectorstore.mongodbatlas.action.MongoDBAtlasLoadAction;
import com.bytechef.component.ai.vectorstore.mongodbatlas.action.MongoDBAtlasSearchAction;
import com.bytechef.component.ai.vectorstore.mongodbatlas.action.MongoDBAtlasUpdateAction;
import com.bytechef.component.ai.vectorstore.mongodbatlas.cluster.MongoDBAtlasSearchTool;
import com.bytechef.component.ai.vectorstore.mongodbatlas.cluster.MongoDBAtlasVectorStore;
import com.bytechef.component.ai.vectorstore.mongodbatlas.connection.MongoDBAtlasConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Alex Bevilacqua
 */
@Component(MONGODB_ATLAS + "_v1_ComponentHandler")
public class MongoDBAtlasComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public MongoDBAtlasComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new MongoDBAtlasComponentDefinitionImpl(
            component(MONGODB_ATLAS)
                .title("MongoDB Atlas Vector Search")
                .description(
                    "MongoDB Atlas Vector Search combines document storage with vector similarity search, enabling " +
                        "storage and retrieval of high-dimensional embeddings for AI and machine learning " +
                        "applications.")
                .icon("path:assets/mongodb-atlas.svg")
                .connection(MongoDBAtlasConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    MongoDBAtlasDeleteAction.of(clusterElementDefinitionService),
                    MongoDBAtlasLoadAction.of(clusterElementDefinitionService),
                    MongoDBAtlasSearchAction.of(clusterElementDefinitionService),
                    MongoDBAtlasUpdateAction.of(clusterElementDefinitionService))
                .clusterElements(
                    MongoDBAtlasSearchTool.of(clusterElementDefinitionService),
                    MongoDBAtlasVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class MongoDBAtlasComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public MongoDBAtlasComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
