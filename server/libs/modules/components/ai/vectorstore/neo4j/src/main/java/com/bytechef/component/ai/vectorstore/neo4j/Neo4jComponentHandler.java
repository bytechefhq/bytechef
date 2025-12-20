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

package com.bytechef.component.ai.vectorstore.neo4j;

import static com.bytechef.component.ai.vectorstore.neo4j.constant.Neo4jConstants.NEO4J;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.neo4j.action.Neo4jLoadAction;
import com.bytechef.component.ai.vectorstore.neo4j.action.Neo4jSearchAction;
import com.bytechef.component.ai.vectorstore.neo4j.cluster.Neo4jVectorStore;
import com.bytechef.component.ai.vectorstore.neo4j.connection.Neo4jConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(NEO4J + "_v1_ComponentHandler")
public class Neo4jComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public Neo4jComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new Neo4jComponentDefinitionImpl(
            component(NEO4J)
                .title("Neo4j")
                .description(
                    "Neo4j is an open-source NoSQL graph database. It is a fully transactional database (ACID) " +
                        "that stores data structured as graphs consisting of nodes, connected by relationships.")
                .icon("path:assets/neo4j.svg")
                .connection(Neo4jConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    new Neo4jLoadAction(clusterElementDefinitionService).actionDefinition,
                    new Neo4jSearchAction(clusterElementDefinitionService).actionDefinition)
                .clusterElements(
                    new Neo4jVectorStore(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class Neo4jComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public Neo4jComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
