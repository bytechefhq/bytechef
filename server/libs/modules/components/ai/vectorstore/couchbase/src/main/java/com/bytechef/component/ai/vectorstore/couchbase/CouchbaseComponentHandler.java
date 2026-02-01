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

package com.bytechef.component.ai.vectorstore.couchbase;

import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.COUCHBASE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.couchbase.action.CouchbaseLoadAction;
import com.bytechef.component.ai.vectorstore.couchbase.action.CouchbaseSearchAction;
import com.bytechef.component.ai.vectorstore.couchbase.cluster.CouchbaseVectorStore;
import com.bytechef.component.ai.vectorstore.couchbase.connection.CouchbaseConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(COUCHBASE + "_v1_ComponentHandler")
public class CouchbaseComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public CouchbaseComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new CouchbaseComponentDefinitionImpl(
            component(COUCHBASE)
                .title("Couchbase")
                .description(
                    "Couchbase is a distributed, JSON document database, with all the desired capabilities of a " +
                        "relational DBMS.")
                .icon("path:assets/couchbase.svg")
                .connection(CouchbaseConnection.CONNECTION_DEFINITION)
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    CouchbaseLoadAction.of(clusterElementDefinitionService),
                    CouchbaseSearchAction.of(clusterElementDefinitionService))
                .clusterElements(
                    CouchbaseVectorStore.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class CouchbaseComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public CouchbaseComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
