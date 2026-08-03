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

package com.bytechef.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;

/**
 * Marker for subagents attached to the Task Tool. Each SUBAGENT cluster element carries its own nested
 * {@link BaseToolFunction#TOOLS} children, so the workflow builder — not the library — decides what a subagent may do.
 *
 * @author Ivica Cardic
 */
public interface SubagentFunction {

    /**
     * The cluster element type under which Task Tool subagents are registered. Optional, because a Task Tool with no
     * subagents attached is valid (it simply has nothing to delegate to), and repeatable, because a builder may define
     * as many subagents as the workflow needs.
     */
    ClusterElementType SUBAGENT = new ClusterElementType("SUBAGENT", "subagent", "Subagent", true, false);
}
