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

package com.bytechef.component.ai.vectorstore.pgvector;

import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.PGVECTOR;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.pgvector.action.PgVectorLoadAction;
import com.bytechef.component.ai.vectorstore.pgvector.action.PgVectorSearchAction;
import com.bytechef.component.ai.vectorstore.pgvector.cluster.PgVectorVectorStore;
import com.bytechef.component.ai.vectorstore.pgvector.connection.PgVectorConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Marko Krišković
 */
@Component(PGVECTOR + "_v1_ComponentHandler")
public class PgVectorComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public PgVectorComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new PGVectorComponentDefinitionImpl(component(PGVECTOR)
            .title("PGVector")
            .description(
                "PGVector is an open-source PostgreSQL extension for vector similarity search.")
            .icon("path:assets/pgvector.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .connection(PgVectorConnection.CONNECTION_DEFINITION)
            .actions(
                PgVectorSearchAction.of(clusterElementDefinitionService),
                PgVectorLoadAction.of(clusterElementDefinitionService))
            .clusterElements(PgVectorVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class PGVectorComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public PGVectorComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
