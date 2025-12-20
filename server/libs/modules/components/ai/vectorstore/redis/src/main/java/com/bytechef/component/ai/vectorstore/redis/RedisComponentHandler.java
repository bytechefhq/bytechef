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

package com.bytechef.component.ai.vectorstore.redis;

import static com.bytechef.component.ai.vectorstore.redis.constant.RedisConstants.REDIS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.vectorstore.redis.action.RedisLoadAction;
import com.bytechef.component.ai.vectorstore.redis.action.RedisSearchAction;
import com.bytechef.component.ai.vectorstore.redis.cluster.RedisVectorStore;
import com.bytechef.component.ai.vectorstore.redis.connection.RedisConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.VectorStoreComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Monika Kušter
 */
@Component(REDIS + "_v1_ComponentHandler")
public class RedisComponentHandler implements ComponentHandler {

    private final VectorStoreComponentDefinition componentDefinition;

    public RedisComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new RedisComponentDefinitionImpl(component(REDIS)
            .title("Redis")
            .description(
                "Redis is an open-source, in-memory data structure store used as a database, cache, and message " +
                    "broker, known for its high performance and support for various data structures like strings, " +
                    "hashes, lists, sets, and more.")
            .icon("path:assets/redis.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .connection(RedisConnection.CONNECTION_DEFINITION)
            .actions(
                new RedisSearchAction(clusterElementDefinitionService).actionDefinition,
                new RedisLoadAction(clusterElementDefinitionService).actionDefinition)
            .clusterElements(new RedisVectorStore(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class RedisComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements VectorStoreComponentDefinition {

        public RedisComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
