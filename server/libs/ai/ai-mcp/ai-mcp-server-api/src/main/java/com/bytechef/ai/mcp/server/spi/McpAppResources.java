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

package com.bytechef.ai.mcp.server.spi;

/**
 * Canonical {@code ui://bytechef/<name>} resource identifiers for the MCP App widgets. Shared between the management
 * server (which registers the resources) and the tool contributors (which reference them in their
 * {@link McpAppUiDescriptor}s) so the two never drift.
 *
 * @author Ivica Cardic
 */
public final class McpAppResources {

    public static final String WORKFLOW_EDITOR_URI = "ui://bytechef/workflow-editor";
    public static final String DATA_TABLE_VIEWER_URI = "ui://bytechef/data-table-viewer";
    public static final String CODE_WORKFLOW_VIEWER_URI = "ui://bytechef/code-workflow-viewer";
    public static final String CUSTOM_COMPONENT_VIEWER_URI = "ui://bytechef/custom-component-viewer";
    public static final String FILE_VIEWER_URI = "ui://bytechef/file-viewer";

    private McpAppResources() {
    }
}
