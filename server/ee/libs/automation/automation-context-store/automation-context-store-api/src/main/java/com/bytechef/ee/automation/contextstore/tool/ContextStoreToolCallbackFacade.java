/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.tool;

import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Mints typed Spring AI {@link ToolCallback} instances for the Context Store, one per
 * {@link com.bytechef.ee.platform.contextstore.domain.ContextStoreSource} belonging to the workspace. Used by the
 * {@code McpServer} enumeration path so external agents (Claude Code, etc.) connecting via MCP see one callback per
 * source with an input schema generated from the source's {@code indexedFields}.
 *
 * <p>
 * Extends the CE-side {@link McpServerWorkspaceToolCallbackContributor} SPI so the CE McpServer configuration can
 * discover the EE implementation without taking a compile-time dependency on the EE module — the EE bean is wired in
 * CE-only-compatible shape via the CE-side interface.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreToolCallbackFacade extends McpServerWorkspaceToolCallbackContributor {

    /**
     * Mints one Spring AI {@link ToolCallback} per enabled
     * {@link com.bytechef.ee.platform.contextstore.domain.ContextStoreSource} for the given workspace. Each callback's
     * name is {@code CONTEXT_STORE_<SOURCE_COMPONENT>_<ENTITY>_SEARCH}; the input schema lists the indexable field
     * names as a JSON Schema {@code enum} on the {@code filters[].field} property.
     *
     * <p>
     * The returned list aggregates both the structured-search callbacks and the semantic-search callbacks (one per
     * source with {@code semanticIndexFields} populated) so the McpServer enumeration sees a single combined surface.
     * When the semantic add-on is absent the semantic callbacks contribute nothing.
     * </p>
     */
    @Override
    List<ToolCallback> getFunctionToolCallbacks(Long workspaceId);

    /**
     * Mints one Spring AI semantic-search {@link ToolCallback} per
     * {@link com.bytechef.ee.platform.contextstore.domain.ContextStoreSource} whose {@code semanticIndexFields} is
     * populated. Returns an empty list when the semantic-search add-on is not active.
     *
     * <p>
     * Each callback's name is {@code CONTEXT_STORE_<SOURCE_COMPONENT>_<ENTITY>_SEMANTIC_SEARCH}; the input schema is
     * {@code {queryText: string, k: integer = 10, prefilter?: ContextStoreQueryFilter}}.
     * </p>
     */
    List<ToolCallback> getSemanticFunctionToolCallbacks(Long workspaceId);
}
