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

package com.bytechef.automation.execution.facade;

import com.bytechef.automation.execution.dto.ToolDTO;
import com.bytechef.platform.mcp.domain.McpTool;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Matija Petanjek
 */
public interface ToolFacade {

    FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(ToolDTO toolDTO);

    List<ToolCallback> getToolCallbacks();

    List<ToolDTO> getTools();

    ToolDTO toTool(McpTool mcpTool);
}
