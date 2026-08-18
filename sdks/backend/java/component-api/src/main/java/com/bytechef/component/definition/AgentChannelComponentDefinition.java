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

package com.bytechef.component.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Exposes the agent channels a component declares, each pairing one of the component's own triggers with an optional
 * reply action.
 *
 * @author Ivica Cardic
 */
public interface AgentChannelComponentDefinition {

    /**
     * Returns the agent channels this component declares.
     * <p>
     * Absent from the serialized definition when none are declared, so that a component which opted into nothing keeps
     * the definition it had before agent channels existed.
     *
     * @return the list of agent channels, or an empty list if none are declared
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    default List<AgentChannelDefinition> getAgentChannels() {
        return List.of();
    }
}
