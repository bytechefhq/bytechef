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

import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Declares that a contributed tool is backed by an MCP App widget. A {@link McpServerToolCallbackContributor} returns
 * one of these per tool name; {@code ManagementMcpServerConfiguration} applies it mechanically — it rebuilds the tool
 * spec with {@code _meta.ui.resourceUri} pointing at {@code resourceUri} and wraps the call handler to inject
 * {@code structuredContent} produced by {@code structuredContentShaper}.
 *
 * <p>
 * The shaper receives the tool result's first text-content string and returns the {@code structuredContent} value the
 * widget reads (or {@code null} to leave the result untouched). Keeping the contract on {@code String} → {@code Map}
 * lets EE contributors own their result-shape knowledge without the CE server importing any EE type.
 * </p>
 *
 * @param resourceUri             the {@code ui://bytechef/<name>} resource the host renders alongside the tool result
 * @param structuredContentShaper maps the tool result text to the widget's {@code structuredContent}, or {@code null}
 *
 * @author Ivica Cardic
 */
public record McpAppUiDescriptor(
    String resourceUri, Function<String, @Nullable Map<String, Object>> structuredContentShaper) {
}
