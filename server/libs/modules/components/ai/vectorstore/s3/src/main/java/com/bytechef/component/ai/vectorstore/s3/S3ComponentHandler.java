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

package com.bytechef.component.ai.vectorstore.s3;

import static com.bytechef.component.ai.vectorstore.s3.constant.S3Constants.S3_VECTOR_STORE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.s3.action.S3LoadAction;
import com.bytechef.component.ai.vectorstore.s3.action.S3SearchAction;
import com.bytechef.component.ai.vectorstore.s3.cluster.S3VectorStore;
import com.bytechef.component.ai.vectorstore.s3.connection.S3Connection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Marko Krišković
 */
@Component(S3_VECTOR_STORE + "_v1_ComponentHandler")
public class S3ComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public S3ComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new S3VectorStoreComponentDefinitionImpl(
            component(S3_VECTOR_STORE)
                .title("S3 Vector Store")
                .description(
                    "Amazon S3 Vector Store uses AWS S3 as a persistent storage backend for vector embeddings, " +
                        "enabling scalable and durable similarity search.")
                .icon("path:assets/s3.svg")
                .connection(S3Connection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    S3LoadAction.of(clusterElementDefinitionService),
                    S3SearchAction.of(clusterElementDefinitionService))
                .clusterElements(
                    S3VectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class S3VectorStoreComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public S3VectorStoreComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
