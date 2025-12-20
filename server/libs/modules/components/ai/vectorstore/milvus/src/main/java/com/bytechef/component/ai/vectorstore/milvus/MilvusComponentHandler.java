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

package com.bytechef.component.ai.vectorstore.milvus;

import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.MILVUS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.milvus.action.MilvusLoadAction;
import com.bytechef.component.ai.vectorstore.milvus.action.MilvusSearchAction;
import com.bytechef.component.ai.vectorstore.milvus.cluster.MilvusVectorStore;
import com.bytechef.component.ai.vectorstore.milvus.connection.MilvusConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(MILVUS + "_v1_ComponentHandler")
public class MilvusComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public MilvusComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new MilvusComponentDefinitionImpl(component(MILVUS)
            .title("Milvus")
            .description(
                "Milvus is an open-source vector database that has garnered significant attention in the fields of " +
                    "data science and machine learning.")
            .icon("path:assets/milvus.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .connection(MilvusConnection.CONNECTION_DEFINITION)
            .actions(
                new MilvusSearchAction(clusterElementDefinitionService).actionDefinition,
                new MilvusLoadAction(clusterElementDefinitionService).actionDefinition)
            .clusterElements(new MilvusVectorStore(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class MilvusComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public MilvusComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
