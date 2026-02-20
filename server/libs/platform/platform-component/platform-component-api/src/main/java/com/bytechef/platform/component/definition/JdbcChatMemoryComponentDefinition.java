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

package com.bytechef.platform.component.definition;

import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface JdbcChatMemoryComponentDefinition extends ClusterRootComponentDefinition {

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(DATA_SOURCE);
    }

    @Override
    default Map<String, List<String>> getActionClusterElementTypes() {
        return Map.of(
            "addMessages", List.of(DATA_SOURCE.name()),
            "getMessages", List.of(DATA_SOURCE.name()),
            "deleteConversation", List.of(DATA_SOURCE.name()),
            "listConversations", List.of(DATA_SOURCE.name()));
    }
}
