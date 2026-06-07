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

package com.bytechef.platform.ai.tool.spi;

import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * SPI implemented by tool sources that contribute Spring AI {@link ToolCallback} instances scoped to a workspace into
 * the McpServer enumeration path. The CE McpServer configuration discovers all beans implementing this interface and
 * appends their callbacks to the per-server tool list, alongside the component-action and workflow tools.
 *
 * @author Ivica Cardic
 */
public interface McpServerWorkspaceToolContributor {

    /**
     * Returns Spring AI {@link ToolCallback} instances contributed by this provider for the given workspace. Returns an
     * empty list when the provider has nothing to contribute.
     *
     * @param workspaceId the workspace whose McpServer is being enumerated
     * @return tool callbacks to expose alongside the component/workflow tools (never {@code null})
     */
    List<ToolCallback> getFunctionToolCallbacks(Long workspaceId);
}
