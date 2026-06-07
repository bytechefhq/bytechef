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

package com.bytechef.component.ai.agent.chat.memory.in.memory.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.in.memory.session.util.InMemorySessionRepositoryHolder;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;

/**
 * @author Ivica Cardic
 */
public class InMemorySessionChatMemory {

    public static ClusterElementDefinition<SessionRepositoryFunction> of() {
        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("In-memory Session Repository")
            .description("Stores session events in memory; cleared on restart.")
            .type(SESSION_REPOSITORY)
            .object(() -> (
                inputParameters, connectionParameters, extensions,
                componentConnections) -> InMemorySessionRepositoryHolder
                    .getInstance());
    }

    private InMemorySessionChatMemory() {
    }
}
