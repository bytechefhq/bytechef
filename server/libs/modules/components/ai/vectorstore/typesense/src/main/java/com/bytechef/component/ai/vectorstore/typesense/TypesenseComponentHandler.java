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

package com.bytechef.component.ai.vectorstore.typesense;

import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.TYPESENSE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.typesense.action.TypesenseLoadAction;
import com.bytechef.component.ai.vectorstore.typesense.action.TypesenseSearchAction;
import com.bytechef.component.ai.vectorstore.typesense.cluster.TypesenseVectorStore;
import com.bytechef.component.ai.vectorstore.typesense.connection.TypesenseConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(TYPESENSE + "_v1_ComponentHandler")
public class TypesenseComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public TypesenseComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new TypesenseComponentDefinitionImpl(component(TYPESENSE)
            .title("Typesense")
            .description(
                "Typesense is an open-source, in-memory search engine designed for fast, typo-tolerant, and " +
                    "relevance-focused full-text search across large datasets.")
            .icon("path:assets/typesense.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .connection(TypesenseConnection.CONNECTION_DEFINITION)
            .actions(
                TypesenseSearchAction.of(clusterElementDefinitionService),
                TypesenseLoadAction.of(clusterElementDefinitionService))
            .clusterElements(TypesenseVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class TypesenseComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public TypesenseComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
