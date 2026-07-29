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
 * Base type shared by tool functions that can be attached to an AI agent as callable tools. It defines the
 * {@link ClusterElementType} under which such tools are registered.
 *
 * @author Ivica Cardic
 */
public interface BaseToolFunction {

    /**
     * The cluster element type under which agent tools are registered.
     */
    ClusterElementType TOOLS = new ClusterElementType("TOOLS", "tools", "Tools", true, false);
}
