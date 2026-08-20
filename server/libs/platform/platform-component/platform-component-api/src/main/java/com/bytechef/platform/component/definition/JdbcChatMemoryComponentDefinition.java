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
import java.util.Collections;
import java.util.LinkedHashMap;
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
        // LinkedHashMap rather than Map.of: Map.of randomises its iteration order per JVM run, so these four keys
        // shuffle in the generated definition snapshot every time anyone regenerates it. Purely cosmetic --
        // JsonFileAssert compares JSON objects order-insensitively -- but it makes each regeneration a spurious diff.
        Map<String, List<String>> actionClusterElementTypes = new LinkedHashMap<>();

        actionClusterElementTypes.put("addMessages", List.of(DATA_SOURCE.name()));
        actionClusterElementTypes.put("getMessages", List.of(DATA_SOURCE.name()));
        actionClusterElementTypes.put("deleteConversation", List.of(DATA_SOURCE.name()));
        actionClusterElementTypes.put("listConversations", List.of(DATA_SOURCE.name()));

        return Collections.unmodifiableMap(actionClusterElementTypes);
    }
}
