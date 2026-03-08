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

package com.bytechef.component.ai.vectorstore.oracle;

import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.ORACLE_VECTOR_STORE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.oracle.action.OracleLoadAction;
import com.bytechef.component.ai.vectorstore.oracle.action.OracleSearchAction;
import com.bytechef.component.ai.vectorstore.oracle.cluster.OracleSearchTool;
import com.bytechef.component.ai.vectorstore.oracle.cluster.OracleVectorStore;
import com.bytechef.component.ai.vectorstore.oracle.connection.OracleConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Marko Krišković
 */
@Component(ORACLE_VECTOR_STORE + "_v1_ComponentHandler")
public class OracleComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public OracleComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new OracleVectorStoreComponentDefinitionImpl(
            component(ORACLE_VECTOR_STORE)
                .title("Oracle Vector Store")
                .description(
                    "Oracle Vector Store uses Oracle Database 23ai's native vector storage and similarity search " +
                        "capabilities to store and query document embeddings.")
                .icon("path:assets/oracle.svg")
                .connection(OracleConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    OracleLoadAction.of(clusterElementDefinitionService),
                    OracleSearchAction.of(clusterElementDefinitionService))
                .clusterElements(
                    OracleSearchTool.of(clusterElementDefinitionService),
                    OracleVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class OracleVectorStoreComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public OracleVectorStoreComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
