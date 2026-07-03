/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Facade for managing {@link AiHubTaskComponent} + {@link AiHubTaskTool} aggregates. Used by the GraphQL controller (UI
 * plus-button menu flow) and the chat affordance callbacks (Attach / Add / Remove).
 *
 * <p>
 * <b>Idempotency contract:</b> {@code attachComponent} returns the existing row when the (task, component, version,
 * connection, environment) tuple already binds — never creates a duplicate. The chat agent can issue "add Slack"
 * repeatedly without proliferating rows.
 * </p>
 *
 * <p>
 * <b>Cascading semantics:</b> deleting a {@code AiHubTaskComponent} cascades to all attached {@code AiHubTaskTool} rows
 * via the FK. Deleting a connection sets {@code connection_id} to NULL on affected components — the binding stays
 * attached to the task but becomes unusable until reconnected (UI surfaces a "Reconnect" chip).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubTaskToolFacade {

    /**
     * Attach a component to the task. Returns the existing component-id when already attached (idempotent), or creates
     * a new binding.
     */
    long attachComponent(
        long taskId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment);

    /**
     * Add (or upsert by name) a configured tool against the binding. Pre-set parameters are stored verbatim and merge
     * into LLM-supplied invocation args at dispatch time.
     */
    long addTool(long taskComponentId, String name, Map<String, ?> parameters);

    /**
     * Update only the parameters of an existing tool. Throws if the tool id doesn't exist.
     */
    void updateToolParameters(long taskToolId, Map<String, ?> parameters);

    /**
     * Rebind an existing component binding to a different connection (or null) in place. Used by the autonomous attach
     * flow to avoid creating a second {@code ai_hub_task_component} row when the LLM first attached with
     * {@code connectionId = null} during discovery and is now back-filling the real connection chosen by the user.
     *
     * <p>
     * Idempotent: setting the connection to the value it already has is a no-op. Throws if the binding id does not
     * exist (the caller is responsible for resolving the existing binding via the repository's task+component lookup).
     * </p>
     */
    void setComponentConnection(long taskComponentId, @Nullable Long connectionId);

    /**
     * Find the {@code taskComponentId} for any existing binding of the (task, component, version, environment) tuple,
     * IGNORING the connection. Returned by the facade rather than a repository directly so the LLM-facing
     * {@code attachTaskTool} callback doesn't need a separate dependency on the repository layer. Used to decide
     * between {@link #setComponentConnection} (rebind in place) and {@link #attachComponent} (new row).
     */
    Optional<Long> findTaskComponentIdIgnoringConnection(
        long taskId, String componentName, int componentVersion, int environment);

    /**
     * Remove a tool from a task. Idempotent: removing a non-existent id is a no-op.
     */
    void removeTool(long taskToolId);

    /**
     * Detach a whole component (cascades to all its tools).
     */
    void detachComponent(long taskComponentId);

    /**
     * Flat listing of every tool attached to the task, joined with its component binding so callers have the
     * (component, connection, environment) context required to construct a callable
     * {@link com.bytechef.ee.ai.hub.task.AiHubTaskToolBinding} per tool.
     */
    List<AiHubTaskToolBinding> listTaskTools(long taskId);

    // --- Per-user (global) "added connectors" -------------------------------------------------------------
    // A user-global binding has task_id NULL and (user_id, workspace_id) set; it applies to every task the user
    // starts. The Connectors page manages these; the resolver unions them with the task's own bindings.

    /**
     * Attach a component as a user-global "added connector" (task_id NULL). Idempotent: one row per (user, workspace,
     * component, version); a repeat call refreshes the connection in place and returns the existing id.
     */
    long attachUserComponent(
        long userId, long workspaceId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment);

    /**
     * The user's globally-added connectors (component bindings with task_id NULL), for the Connectors page.
     */
    List<AiHubTaskComponent> listUserComponents(long userId, long workspaceId);

    /**
     * The stored tool rows for a component binding — deviations from the all-tools-enabled default (disabled tools,
     * and/or tools with configured parameters). The Connectors page reads these to render per-tool toggles; a tool with
     * no row is enabled.
     */
    List<AiHubTaskTool> listComponentTools(long taskComponentId);

    /**
     * Flat listing of the ENABLED tools across the user's globally-added connectors (component.enabled &&
     * tool.enabled), as callable bindings. The resolver unions these with the task's own tools at dispatch. The
     * binding's {@code taskId} is a 0 sentinel — these tools aren't task-scoped and the callback doesn't key off it.
     */
    List<AiHubTaskToolBinding> listUserTools(long userId, long workspaceId);

    /**
     * Toggle a component binding on/off (the component-level enabled flag). Throws if the id doesn't exist.
     */
    void setComponentEnabled(long taskComponentId, boolean enabled);

    /**
     * Toggle a single tool on/off under a component binding. Upserts an {@link AiHubTaskTool} row carrying the enabled
     * flag — a missing row defaults to enabled — preserving any configured parameters.
     */
    void setToolEnabled(long taskComponentId, String toolName, boolean enabled);

    /**
     * Set the pre-configured parameters for a single tool under a component binding. Upserts an {@link AiHubTaskTool}
     * row carrying the parameters — a missing row defaults to enabled — preserving the enabled flag.
     */
    void setToolParameters(long taskComponentId, String toolName, Map<String, ?> parameters);
}
