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

package com.bytechef.platform.tool.execution;

/**
 * The surface a direct tool/action execution was invoked through. Persisted by ordinal; append new values at the end
 * (pinned by an enum ordinal stability test).
 *
 * @author Ivica Cardic
 */
public enum ToolExecutionSurface {

    MCP_AUTOMATION,
    MCP_EMBEDDED,
    MCP_MANAGEMENT,
    EMBEDDED_API_ACTION,
    EMBEDDED_API_TOOL
}
