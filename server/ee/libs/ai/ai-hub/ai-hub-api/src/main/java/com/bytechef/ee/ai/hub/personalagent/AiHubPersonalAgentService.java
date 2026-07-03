/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Service surface for {@link AiHubPersonalAgent} CRUD. Owns slug normalisation, ownership checks, and the
 * find-or-create idempotency boundary so a controller / tool callback never has to reason about the unique-constraint
 * race directly.
 *
 * <p>
 * <b>Ownership invariant:</b> every read and write requires both {@code workspaceId} and {@code userId}. The agent row
 * itself is workspace-agnostic; workspace association lives on {@link WorkspaceAiHubPersonalAgent} and the service
 * joins through it for every query and re-validates membership on every mutation so a forged id from the client cannot
 * reach an agent attached to another workspace or owned by another user. {@code (user, environment, name)} is no longer
 * DB-unique — the service-layer duplicate-name check in {@code create} is the only gate, and a concurrent same-name
 * create CAN race; the resulting redundant row is harmless and removable from the UI.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubPersonalAgentService {

    /**
     * Returns every personal agent for the given workspace+user+environment, sorted most-recently-updated first. Empty
     * list when none exist (the sidebar handles that with a "Create your first agent" CTA rather than an error).
     */
    List<AiHubPersonalAgent> list(long workspaceId, long userId, int environment);

    /**
     * Returns the agent identified by id, but only if it belongs to the given workspace+user. The redundant ownership
     * check prevents a client that forged an id from reaching another user's agent — even though the controller's
     *
     * @PreAuthorize already gates by workspace, defense-in-depth at the service layer catches missed annotations.
     */
    Optional<AiHubPersonalAgent> findOwned(long agentId, long workspaceId, long userId);

    /**
     * Returns the agent matching the (workspace, user, environment, name) tuple, or empty if no row matches. Used by
     * the create-or-resolve tool flow to short-circuit a duplicate-name attempt before incurring a DB constraint
     * violation.
     */
    Optional<AiHubPersonalAgent> findByName(long workspaceId, long userId, int environment, String name);

    /**
     * Creates a new personal agent. Slug normalisation happens at the service layer — callers can pass either a
     * pre-slugified name or a free-text name; the implementation enforces the {@link AiHubPersonalAgent#NAME_PATTERN}
     * regex (throwing {@link IllegalArgumentException} on a value that cannot be slugified). Throws
     * {@code com.bytechef.exception.ConflictException} when a row with the same name already exists for this
     * workspace/user/environment.
     *
     * <p>
     * Per-agent LLM model override: {@code llmProvider} and {@code llmModel} together pick a model that overrides the
     * workspace default for this agent. Must both be set or both be null — setting one without the other throws
     * {@link IllegalArgumentException}. When set, the implementation does NOT verify the (provider, model) pair exists
     * in the workspace's available providers; that check is deferred to the per-turn agent run, so admin can still save
     * the row when temporarily reorganising provider catalogs.
     */
    AiHubPersonalAgent create(
        long workspaceId, long userId, int environment, String name, @Nullable String title,
        @Nullable String description, @Nullable String instructions, @Nullable String llmProvider,
        @Nullable String llmModel);

    /**
     * Patches the editable fields of an existing personal agent. Each parameter is optional — {@code null} leaves the
     * existing value untouched. Returns the updated row. The agent's {@code name} is intentionally NOT updatable here
     * because chat URLs and tool references would silently break; renames must go through a dedicated rename operation
     * that updates dependent references.
     *
     * <p>
     * The {@code llmProvider} / {@code llmModel} pair follows tri-state semantics: omit both = no change; set both =
     * apply; set only one = {@link IllegalArgumentException}. To CLEAR a previously set override, pass {@code ""}
     * (empty string) for both — null on this argument means "leave existing untouched," not "clear."
     */
    AiHubPersonalAgent update(
        long agentId, long workspaceId, long userId, @Nullable String title, @Nullable String description,
        @Nullable String instructions, @Nullable String llmProvider, @Nullable String llmModel);

    /**
     * Deletes the agent identified by id. Throws when the agent doesn't belong to the given workspace+user. The
     * implementation also archives any open tasks bound to this agent so the sidebar doesn't show ghost rows pointing
     * at a deleted definition.
     */
    void delete(long agentId, long workspaceId, long userId);

    /**
     * Lists the tool template rows associated with the given agent, ordered by creation time. Returns an empty list if
     * the agent has no tools (or doesn't exist). The caller is expected to have ownership-validated the agent id via
     * {@link #findOwned} or equivalent.
     */
    List<AiHubPersonalAgentTool> listTools(long agentId);

    /**
     * Adds a tool template row to the agent. Idempotent: re-adding the same (componentName, componentVersion,
     * operationName) tuple returns the existing row rather than creating a duplicate, matching the partial unique
     * index. Throws when the agent doesn't belong to the caller — defense in depth alongside the controller's ownership
     * check.
     */
    AiHubPersonalAgentTool addTool(
        long agentId, long workspaceId, long userId, String componentName, int componentVersion,
        String operationName);

    /**
     * Removes the tool template row by id. The id is verified to belong to an agent owned by the caller before the
     * delete fires, so a forged id from the client cannot reach another user's tool template. Idempotent: deleting an
     * already-deleted (or non-existent) tool is a no-op rather than an error.
     */
    void removeTool(long toolId, long workspaceId, long userId);

    /**
     * Updates the per-tool config (pinned connection + pre-set parameters) on the given tool template row.
     *
     * <p>
     * Both fields are independently nullable in the input:
     * </p>
     * <ul>
     * <li>{@code connectionId} — pin the tool to a specific connection so every spawned task runs against the same
     * authenticated workspace. {@code null} clears the binding (matches the schema's nullable column +
     * {@code ON DELETE SET NULL} fallback for connection-deletion). Verified to be a connection the caller can use
     * (cross-tenant probe protection) before the update applies.</li>
     * <li>{@code parameters} — pre-seed the LLM-invocation argument map. {@code null} preserves the existing map
     * (rather than clearing); pass an explicit empty map to reset. Stored verbatim, no shape validation against the
     * action's input schema (the LLM's invocation args still merge on top, and over-specified keys are inert).</li>
     * </ul>
     *
     * <p>
     * Ownership: the tool id is resolved to its parent agent and the agent's (workspaceId, userId) is verified against
     * the caller before any mutation. A forged tool id from the client therefore can't reach another user's row. Throws
     * {@code com.bytechef.exception.NotFoundException} when no tool with the id exists for the caller.
     * </p>
     *
     * @return the updated row so the controller can return it through the GraphQL response without a follow-up read
     */
    AiHubPersonalAgentTool updateToolConfig(
        long toolId, long workspaceId, long userId, @Nullable Long connectionId, @Nullable Map<String, ?> parameters);

    /**
     * Lists the resource template rows associated with the given agent, ordered by creation time. Returns an empty list
     * if the agent has no resources (or doesn't exist). The caller is expected to have ownership-validated the agent id
     * via {@link #findOwned} or equivalent.
     */
    List<AiHubPersonalAgentResource> listResources(long agentId);

    /**
     * Adds a resource template row to the agent. Idempotent: re-adding the same (kind, resourceId) for the agent
     * returns the existing row rather than creating a duplicate, matching the unique constraint. Throws when the agent
     * doesn't belong to the caller — defense in depth alongside the controller's ownership check.
     */
    AiHubPersonalAgentResource addResource(
        long agentId, long workspaceId, long userId, AiHubPersonalAgentResourceKind kind, String resourceId,
        String resourceName);

    /**
     * Removes the resource template row by id. The id is verified to belong to an agent owned by the caller before the
     * delete fires. Idempotent: removing an already-deleted (or non-existent) resource is a no-op.
     */
    void removeResource(long personalAgentResourceId, long workspaceId, long userId);

    /**
     * Clones the personal agent identified by {@code agentId} into {@code targetEnvironment} as a new
     * {@link AiHubPersonalAgent} row, preserving title, description, and instructions verbatim and deep-copying every
     * {@link AiHubPersonalAgentTool} template entry. The slug ({@code name}) defaults to the source agent's name when
     * {@code newName} is null; if a row with the resolved (workspace, user, environment, name) tuple already exists,
     * throws {@code com.bytechef.ee.ai.hub.exception.ConflictException} so the caller can prompt the user (or LLM) for
     * a different name rather than silently overwriting.
     *
     * <p>
     * Same-workspace, cross-environment scope: the source agent's {@code workspaceId} and {@code userId} carry over to
     * the clone. Cross-workspace cloning would require an additional permission check and is intentionally out of scope
     * for v1 — the user's mental model is "promote my agent to PROD," not "share it with another workspace."
     *
     * @param agentId           id of the source agent (must exist and belong to {@code (workspaceId, userId)})
     * @param workspaceId       owning workspace — verified against the source agent for defense-in-depth
     * @param userId            owning user — verified against the source agent
     * @param targetEnvironment environment ordinal of the destination ({@link Environment#ordinal()})
     * @param newName           optional override for the clone's slug name; falls back to the source name when null
     * @return the persisted clone
     */
    AiHubPersonalAgent cloneToEnvironment(
        long agentId, long workspaceId, long userId, int targetEnvironment, @Nullable String newName);
}
