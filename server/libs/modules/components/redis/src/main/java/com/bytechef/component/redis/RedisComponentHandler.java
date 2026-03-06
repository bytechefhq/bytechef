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

package com.bytechef.component.redis;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.redis.action.RedisDeleteAction;
import com.bytechef.component.redis.action.RedisGetAction;
import com.bytechef.component.redis.action.RedisIncrementAction;
import com.bytechef.component.redis.action.RedisInfoAction;
import com.bytechef.component.redis.action.RedisKeysAction;
import com.bytechef.component.redis.action.RedisListLengthAction;
import com.bytechef.component.redis.action.RedisPopAction;
import com.bytechef.component.redis.action.RedisPublishAction;
import com.bytechef.component.redis.action.RedisPushAction;
import com.bytechef.component.redis.action.RedisSetAction;
import com.bytechef.component.redis.connection.RedisConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class RedisComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("redis")
        .title("Redis")
        .description(
            "Redis is an open-source, in-memory data structure store used as a database, cache, message broker, " +
                "and streaming engine.")
        .icon("path:assets/redis.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(RedisConnection.CONNECTION_DEFINITION)
        .actions(
            RedisDeleteAction.ACTION_DEFINITION,
            RedisGetAction.ACTION_DEFINITION,
            RedisIncrementAction.ACTION_DEFINITION,
            RedisInfoAction.ACTION_DEFINITION,
            RedisKeysAction.ACTION_DEFINITION,
            RedisListLengthAction.ACTION_DEFINITION,
            RedisPopAction.ACTION_DEFINITION,
            RedisPublishAction.ACTION_DEFINITION,
            RedisPushAction.ACTION_DEFINITION,
            RedisSetAction.ACTION_DEFINITION)
        .clusterElements(
            tool(RedisDeleteAction.ACTION_DEFINITION),
            tool(RedisGetAction.ACTION_DEFINITION),
            tool(RedisIncrementAction.ACTION_DEFINITION),
            tool(RedisInfoAction.ACTION_DEFINITION),
            tool(RedisKeysAction.ACTION_DEFINITION),
            tool(RedisListLengthAction.ACTION_DEFINITION),
            tool(RedisPopAction.ACTION_DEFINITION),
            tool(RedisPublishAction.ACTION_DEFINITION),
            tool(RedisPushAction.ACTION_DEFINITION),
            tool(RedisSetAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
