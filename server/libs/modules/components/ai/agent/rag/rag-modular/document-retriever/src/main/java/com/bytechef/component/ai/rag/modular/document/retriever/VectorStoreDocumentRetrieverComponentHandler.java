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

package com.bytechef.component.ai.rag.modular.document.retriever;

import static com.bytechef.component.ai.rag.modular.document.retriever.VectorStoreDocumentRetrieverComponentHandler.VECTOR_STORE_DOCUMENT_RETRIEVER;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.rag.modular.document.retriever.cluster.VectorStoreDocumentRetriever;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreDocumentRetrieverComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(VECTOR_STORE_DOCUMENT_RETRIEVER + "_v1_ComponentHandler")
public class VectorStoreDocumentRetrieverComponentHandler implements ComponentHandler {

    public static final String VECTOR_STORE_DOCUMENT_RETRIEVER = "vectorStoreDocumentRetriever";

    private final VectorStoreDocumentRetrieverComponentDefinition componentDefinition;

    public VectorStoreDocumentRetrieverComponentHandler(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        this.componentDefinition = new VectorStoreDocumentRetrieverComponentDefinitionImpl(
            component(VECTOR_STORE_DOCUMENT_RETRIEVER)
                .title("Vector Store Document Retriever")
                .description("Vector Store Document Retriever.")
                .icon("path:assets/document-retriever.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new VectorStoreDocumentRetriever(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class VectorStoreDocumentRetrieverComponentDefinitionImpl
        extends AbstractComponentDefinitionWrapper
        implements VectorStoreDocumentRetrieverComponentDefinition {

        public VectorStoreDocumentRetrieverComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
