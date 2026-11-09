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
import com.bytechef.component.ai.vectorstore.s3.action.S3DeleteActionV2;
import com.bytechef.component.ai.vectorstore.s3.action.S3LoadActionV2;
import com.bytechef.component.ai.vectorstore.s3.action.S3SearchActionV2;
import com.bytechef.component.ai.vectorstore.s3.action.S3UpdateActionV2;
import com.bytechef.component.ai.vectorstore.s3.cluster.S3SearchToolV2;
import com.bytechef.component.ai.vectorstore.s3.cluster.S3VectorStoreV2;
import com.bytechef.component.ai.vectorstore.s3.connection.S3ConnectionV2;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(S3_VECTOR_STORE + "_v2_ComponentHandler")
public class S3ComponentHandlerV2 implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public S3ComponentHandlerV2(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new S3VectorStoreComponentDefinitionImpl(
            component(S3_VECTOR_STORE)
                .title("S3 Vector Store")
                .description(
                    "Amazon S3 Vector Store uses Amazon S3 Vectors, a purpose-built vector bucket and index, to " +
                        "store vector embeddings and run scalable, durable similarity search.")
                .icon("path:assets/s3.svg")
                .version(2)
                .connection(S3ConnectionV2.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    S3DeleteActionV2.of(clusterElementDefinitionService),
                    S3LoadActionV2.of(clusterElementDefinitionService),
                    S3SearchActionV2.of(clusterElementDefinitionService),
                    S3UpdateActionV2.of(clusterElementDefinitionService))
                .clusterElements(
                    S3SearchToolV2.of(clusterElementDefinitionService),
                    S3VectorStoreV2.of(clusterElementDefinitionService)));
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
