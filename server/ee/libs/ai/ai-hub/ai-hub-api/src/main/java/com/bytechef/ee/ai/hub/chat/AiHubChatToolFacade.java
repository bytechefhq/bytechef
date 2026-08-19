/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Facade for managing {@link AiHubChatComponent} + {@link AiHubChatTool} aggregates. Used by the GraphQL controller (UI
 * plus-button menu flow) and the chat affordance callbacks (Attach / Add / Remove).
 *
 * <p>
 * <b>Idempotency contract:</b> {@code attachComponent} returns the existing row when the (chat, component, version,
 * connection, environment) tuple already binds — never creates a duplicate. The chat agent can issue "add Slack"
 * repeatedly without proliferating rows.
 * </p>
 *
 * <p>
 * <b>Cascading semantics:</b> deleting a {@code AiHubChatComponent} cascades to all attached {@code AiHubChatTool} rows
 * via the FK. Deleting a connection sets {@code connection_id} to NULL on affected components — the binding stays
 * attached to the chat but becomes unusable until reconnected (UI surfaces a "Reconnect" chip).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatToolFacade {

    /**
     * Attach a component to the chat. Returns the existing component-id when already attached (idempotent), or creates
     * a new binding.
     */
    long attachComponent(
        long chatId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment);

    /**
     * Add (or upsert by name) a configured tool against the binding. Pre-set parameters are stored verbatim and merge
     * into LLM-supplied invocation args at dispatch time.
     */
    long addTool(long chatComponentId, String name, Map<String, ?> parameters);

    /**
     * Update only the parameters of an existing tool. Throws if the tool id doesn't exist.
     */
    void updateToolParameters(long chatToolId, Map<String, ?> parameters);

    /**
     * Rebind an existing component binding to a different connection (or null) in place. Used by the autonomous attach
     * flow to avoid creating a second {@code ai_hub_chat_component} row when the LLM first attached with
     * {@code connectionId = null} during discovery and is now back-filling the real connection chosen by the user.
     *
     * <p>
     * Idempotent: setting the connection to the value it already has is a no-op. Throws if the binding id does not
     * exist (the caller is responsible for resolving the existing binding via the repository's chat+component lookup).
     * </p>
     */
    void setComponentConnection(long chatComponentId, @Nullable Long connectionId);

    /**
     * Find the {@code chatComponentId} for any existing binding of the (chat, component, version, environment) tuple,
     * IGNORING the connection. Returned by the facade rather than a repository directly so the LLM-facing
     * {@code attachChatTool} callback doesn't need a separate dependency on the repository layer. Used to decide
     * between {@link #setComponentConnection} (rebind in place) and {@link #attachComponent} (new row).
     */
    Optional<Long> findChatComponentIdIgnoringConnection(
        long chatId, String componentName, int componentVersion, int environment);

    /**
     * Remove a tool from a chat. Idempotent: removing a non-existent id is a no-op.
     */
    void removeTool(long chatToolId);

    /**
     * Detach a whole component (cascades to all its tools).
     */
    void detachComponent(long chatComponentId);

    /**
     * Flat listing of every tool attached to the chat, joined with its component binding so callers have the
     * (component, connection, environment) context required to construct a callable
     * {@link com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding} per tool.
     */
    List<AiHubChatToolBinding> listChatTools(long chatId);

    // --- Per-user (global) "added connectors" -------------------------------------------------------------
    // A user-global binding has chat_id NULL and (user_id, workspace_id) set; it applies to every chat the user
    // starts. The Connectors page manages these; the resolver unions them with the chat's own bindings.

    /**
     * Attach a component as a user-global "added connector" (chat_id NULL). Idempotent: one row per (user, workspace,
     * component, version); a repeat call refreshes the connection in place and returns the existing id.
     */
    long attachUserComponent(
        long userId, long workspaceId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment);

    /**
     * The user's globally-added connectors (component bindings with chat_id NULL), for the Connectors page.
     */
    List<AiHubChatComponent> listUserComponents(long userId, long workspaceId);

    /**
     * The stored tool rows for a component binding — deviations from the all-tools-enabled default (disabled tools,
     * and/or tools with configured parameters). The Connectors page reads these to render per-tool toggles; a tool with
     * no row is enabled.
     */
    List<AiHubChatTool> listComponentTools(long chatComponentId);

    /**
     * Flat listing of the ENABLED tools across the user's globally-added connectors (component.enabled &&
     * tool.enabled), as callable bindings. The resolver unions these with the chat's own tools at dispatch. The
     * binding's {@code chatId} is a 0 sentinel — these tools aren't chat-scoped and the callback doesn't key off it.
     */
    List<AiHubChatToolBinding> listUserTools(long userId, long workspaceId);

    /**
     * Toggle a component binding on/off (the component-level enabled flag). Throws if the id doesn't exist.
     */
    void setComponentEnabled(long chatComponentId, boolean enabled);

    // --- Per-chat participation -----------------------------------------------------------------------------
    // A user-global connector says the connector is AVAILABLE; whether it acts in one particular chat is a
    // separate decision, stored in ai_hub_chat_connector. See AiHubChatConnector for why that is its own table
    // rather than a chat-scoped ai_hub_chat_component row.

    /**
     * Turn a user-global connector on/off FOR ONE CHAT, leaving its user-global availability untouched.
     *
     * <p>
     * Idempotent, and re-enabling deletes nothing: a row with {@code enabled = true} is indistinguishable from no row
     * at all, since absence means participating.
     * </p>
     */
    void setChatConnectorEnabled(long chatId, long userConnectorId, boolean enabled);

    /**
     * The component names the chat has switched OFF. Callers subtract these from the user-global tool set — the
     * suppression does NOT happen on its own, because {@link #listUserTools} knows nothing about one chat and the
     * resolver unions both sets.
     */
    Set<String> listChatDisabledConnectors(long chatId);

    /**
     * Toggle a single tool on/off under a component binding. Upserts an {@link AiHubChatTool} row carrying the enabled
     * flag — a missing row defaults to enabled — preserving any configured parameters.
     */
    void setToolEnabled(long chatComponentId, String toolName, boolean enabled);

    /**
     * Set the pre-configured parameters for a single tool under a component binding. Upserts an {@link AiHubChatTool}
     * row carrying the parameters — a missing row defaults to enabled — preserving the enabled flag.
     */
    void setToolParameters(long chatComponentId, String toolName, Map<String, ?> parameters);
}
