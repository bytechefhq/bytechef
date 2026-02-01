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

package com.bytechef.component.ai.vectorstore.redis.action;

import static com.bytechef.component.ai.vectorstore.redis.constant.RedisConstants.REDIS;

import com.bytechef.component.ai.vectorstore.action.AbstractLoadAction;
import com.bytechef.component.ai.vectorstore.redis.constant.RedisConstants;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class RedisLoadAction {

    public static ActionDefinition of(ClusterElementDefinitionService clusterElementDefinitionService) {
        return AbstractLoadAction.of(
            REDIS, RedisConstants.VECTOR_STORE, List.of(), clusterElementDefinitionService);
    }
}
