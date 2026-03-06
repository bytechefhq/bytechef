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

package com.bytechef.component.ai.vectorstore.mariadb;

import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.MARIA_DB_VECTOR_STORE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.mariadb.action.MariaDBVectorStoreLoadAction;
import com.bytechef.component.ai.vectorstore.mariadb.action.MariaDBVectorStoreSearchAction;
import com.bytechef.component.ai.vectorstore.mariadb.cluster.MariaDBVectorStoreClusterElement;
import com.bytechef.component.ai.vectorstore.mariadb.connection.MariaDBVectorStoreConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Marko Krišković
 */
@Component(MARIA_DB_VECTOR_STORE + "_v1_ComponentHandler")
public class MariaDBVectorStoreComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public MariaDBVectorStoreComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new MariaDBVectorStoreComponentDefinitionImpl(
            component(MARIA_DB_VECTOR_STORE)
                .title("MariaDB Vector Store")
                .description(
                    "MariaDB Vector Store uses MariaDB 11.7+ native vector storage and similarity search " +
                        "capabilities to store and query document embeddings.")
                .icon("path:assets/mariadb-vector-store.svg")
                .connection(MariaDBVectorStoreConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    MariaDBVectorStoreLoadAction.of(clusterElementDefinitionService),
                    MariaDBVectorStoreSearchAction.of(clusterElementDefinitionService))
                .clusterElements(
                    MariaDBVectorStoreClusterElement.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class MariaDBVectorStoreComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public MariaDBVectorStoreComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
