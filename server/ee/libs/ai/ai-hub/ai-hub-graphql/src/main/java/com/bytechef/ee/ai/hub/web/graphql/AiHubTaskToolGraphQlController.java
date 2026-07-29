/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskComponent;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskTool;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolBinding;
import com.bytechef.ee.ai.hub.task.AiHubTaskToolFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for the per-task tool persistence (v2.4 client UI). Mirrors the chat-driven affordance shapes
 * ({@code AttachTaskToolToolCallback}, {@code RemoveTaskToolToolCallback}) so the LLM and the UI go through the same
 * {@link AiHubTaskToolFacade} contract — neither side has its own private mutation path.
 *
 * <p>
 * The {@code aiHubTaskToolableComponents} query bundles the catalog walk in one round-trip so the composer plus-button
 * menu can render a hierarchical Component → Tools tree without N+1 fetches. Tool-less components (no cluster element
 * typed as TOOLS) are filtered out — they have nothing to show in the menu.
 * </p>
 *
 * <p>
 * All operations are workspace-scoped and require the caller to have access to the supplied workspace. AiHubTask
 * ownership is verified at the service layer ({@link AiHubTaskService#getById(long, long, long)}) which throws on
 * cross-user / cross-workspace mismatch.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubTaskToolGraphQlController {

    private static final int DEFAULT_COMPONENT_VERSION = 1;

    private final AiHubTaskService taskService;
    private final AiHubTaskToolFacade taskToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubTaskToolGraphQlController(
        AiHubTaskService taskService,
        AiHubTaskToolFacade taskToolFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, UserService userService,
        WorkspaceFacade workspaceFacade) {

        this.taskService = taskService;
        this.taskToolFacade = taskToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @QueryMapping
    public List<AiHubTaskToolBinding> aiHubTaskTools(
        @Argument long workspaceId, @Argument String taskId) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // The client-supplied "taskId" is the AG-UI threadId (NanoID) carried by useAiHubStore — see
        // the schema doc on AiHubTask.threadId. The composer renders this chip list before the user
        // sends
        // their first message, so the task row may not exist in the DB yet (it's created by the chat
        // backend on the first turn). Treat a not-yet-persisted thread as "no attached tools" rather than 404 —
        // there is definitionally nothing to list. Probe-oracle defense still holds: a thread that exists but
        // belongs to another user also returns an empty list, indistinguishable from "not yet created".
        return taskService.findByThreadId(taskId)
            .filter(task -> task.getUserId() == userId
                && taskService.getWorkspaceId(task.getId()) == workspaceId)
            .map(task -> taskToolFacade.listTaskTools(task.getId()))
            .orElseGet(List::of);
    }

    @QueryMapping
    public List<ToolableComponent> aiHubTaskToolableComponents(@Argument long workspaceId) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Walk the curated TOOLS-typed cluster elements and group by (componentName, componentVersion).
        // Each toolable component carries the PARENT COMPONENT's title/description/icon (e.g. "Notion",
        // "Slack") — NOT the cluster element's, which would surface action labels like "Add Block to
        // Page" or "Append Row" as if they were components. We resolve the parent definition lazily and
        // cache it per (name, version) to keep the catalog walk to one lookup per unique component.
        List<ClusterElementDefinition> toolDefinitions =
            clusterElementDefinitionService.getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);

        Map<String, ToolableComponent> byComponent = new HashMap<>();
        Map<String, ComponentDefinition> componentCache = new HashMap<>();

        for (ClusterElementDefinition definition : toolDefinitions) {
            String key = definition.getComponentName() + "#" + definition.getComponentVersion();

            ComponentDefinition componentDefinition = componentCache.computeIfAbsent(
                key,
                ck -> componentDefinitionService.getComponentDefinition(
                    definition.getComponentName(), definition.getComponentVersion()));

            // The connectors list is for components you CONNECT to: skip cluster roots (e.g. AI Agent — it
            // composes other tools rather than being one) and components without a connection definition
            // (logic, approval, agent utils, …). Those are still usable as tools; they just aren't connectors.
            if (componentDefinition.isClusterRoot() || componentDefinition.getConnection() == null) {
                continue;
            }

            ToolableComponent component = byComponent.computeIfAbsent(
                key,
                k -> new ToolableComponent(
                    definition.getComponentName(), definition.getComponentVersion(),
                    componentDefinition.getTitle(), componentDefinition.getDescription(),
                    componentDefinition.getIcon(), new ArrayList<>(),
                    componentDefinition.isConnectionRequired()));

            component.tools()
                .add(new ToolableClusterElement(
                    definition.getName(), definition.getTitle(), definition.getDescription()));
        }

        return new ArrayList<>(byComponent.values());
    }

    @MutationMapping
    public AiHubTaskToolBinding
        attachAiHubTaskTool(@Argument AttachAiHubTaskToolInput input) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, input.workspaceId());

        // Same threadId resolution as the read path; see the aiHubTaskTools javadoc.
        AiHubTask task = taskService.getByThreadId(
            input.taskId(), input.workspaceId(), userId);

        int componentVersion = input.componentVersion() == null ? DEFAULT_COMPONENT_VERSION : input.componentVersion();

        long taskComponentId = taskToolFacade.attachComponent(
            task.getId(), input.componentName(), componentVersion, input.connectionId(),
            input.environment());

        Map<String, ?> parameters = input.parameters() == null ? Map.of() : input.parameters();

        long taskToolId = taskToolFacade.addTool(
            taskComponentId, input.clusterElementName(), parameters);

        // Re-list and find the row we just upserted so the response includes the joined component context
        // (saves the client a follow-up query). Linear scan is fine — a task has at most a few
        // dozen attached tools.
        return taskToolFacade.listTaskTools(task.getId())
            .stream()
            .filter(binding -> binding.taskToolId() == taskToolId)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Just-attached tool " + taskToolId + " not found in task listing"));
    }

    @MutationMapping
    public AiHubTaskToolBinding updateAiHubTaskToolParameters(
        @Argument long workspaceId, @Argument long taskToolId, @Argument Map<String, Object> parameters) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Resolve the task that owns this tool so we can rebuild the binding for the response and
        // verify ownership against (workspaceId, userId). Skipping the ownership check would let any
        // workspace member edit any tool's parameters by id.
        AiHubTaskToolBinding existing = findToolBindingOrThrow(workspaceId, userId, taskToolId);

        taskToolFacade.updateToolParameters(taskToolId, parameters);

        return findToolBindingOrThrow(workspaceId, userId, taskToolId, existing.taskId());
    }

    @MutationMapping
    public boolean removeAiHubTaskTool(@Argument long workspaceId, @Argument long taskToolId) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Same ownership-check pattern as the update path.
        AiHubTaskToolBinding existing = findToolBindingOrThrow(workspaceId, userId, taskToolId);

        taskToolFacade.removeTool(existing.taskToolId());

        return true;
    }

    @MutationMapping
    public boolean detachAiHubTaskComponent(
        @Argument long workspaceId, @Argument long taskComponentId) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        // Detach is destructive (cascades to all tools), so verify ownership before the cascade. Look for
        // any binding under this component in the user's workspace aiHubTasks; if found, the component
        // is owned and detach can proceed. If not, the component either doesn't exist or belongs to
        // another user — return false (idempotent no-op) so a probe can't enumerate component ids.
        AiHubTaskToolBinding owningBinding =
            findAnyBindingForComponent(workspaceId, userId, taskComponentId);

        if (owningBinding == null) {
            return false;
        }

        taskToolFacade.detachComponent(taskComponentId);

        return true;
    }

    @QueryMapping
    public List<AiHubUserConnector> aiHubUserConnectors(@Argument long workspaceId) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        List<AiHubTaskComponent> components = taskToolFacade.listUserComponents(userId, workspaceId);

        if (components.isEmpty()) {
            return List.of();
        }

        Map<String, List<ToolableClusterElement>> toolsByComponent = buildToolsByComponent();
        Map<String, ComponentDefinition> componentCache = new HashMap<>();

        List<AiHubUserConnector> connectors = new ArrayList<>();

        for (AiHubTaskComponent component : components) {
            String key = component.getComponentName() + "#" + component.getComponentVersion();

            ComponentDefinition componentDefinition = componentCache.computeIfAbsent(
                key,
                ck -> componentDefinitionService.getComponentDefinition(
                    component.getComponentName(), component.getComponentVersion()));

            // A stored tool row with enabled=false is a deviation from the all-tools-enabled default; a row may
            // also carry pre-configured parameters. Index both by tool name so the catalog walk can surface them.
            Set<String> disabledTools = new HashSet<>();
            Map<String, Map<String, ?>> parametersByTool = new HashMap<>();

            for (AiHubTaskTool tool : taskToolFacade.listComponentTools(component.getId())) {
                if (!tool.isEnabled()) {
                    disabledTools.add(tool.getName());
                }

                Map<String, ?> parameters = tool.getParameters();

                if (parameters != null && !parameters.isEmpty()) {
                    parametersByTool.put(tool.getName(), parameters);
                }
            }

            List<AiHubUserConnectorTool> tools = toolsByComponent.getOrDefault(key, List.of())
                .stream()
                .map(catalogTool -> new AiHubUserConnectorTool(
                    catalogTool.name(), catalogTool.title(), catalogTool.description(),
                    !disabledTools.contains(catalogTool.name()), parametersByTool.get(catalogTool.name())))
                .toList();

            connectors.add(
                new AiHubUserConnector(
                    component.getId(), component.getComponentName(), component.getComponentVersion(),
                    componentDefinition.getTitle(), componentDefinition.getDescription(),
                    componentDefinition.getIcon(), component.getConnectionId(),
                    componentDefinition.isConnectionRequired(), component.isEnabled(), tools));
        }

        return connectors;
    }

    @MutationMapping
    public String addAiHubUserConnector(
        @Argument long workspaceId, @Argument String componentName, @Argument int componentVersion,
        @Argument @Nullable Long connectionId, @Argument int environment) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        long connectorId = taskToolFacade.attachUserComponent(
            userId, workspaceId, componentName, componentVersion, connectionId, environment);

        return String.valueOf(connectorId);
    }

    @MutationMapping
    public boolean removeAiHubUserConnector(@Argument long workspaceId, @Argument long connectorId) {
        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        taskToolFacade.detachComponent(connectorId);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorEnabled(
        @Argument long workspaceId, @Argument long connectorId, @Argument boolean enabled) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        taskToolFacade.setComponentEnabled(connectorId, enabled);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorToolEnabled(
        @Argument long workspaceId, @Argument long connectorId, @Argument String toolName,
        @Argument boolean enabled) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        taskToolFacade.setToolEnabled(connectorId, toolName, enabled);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorToolParameters(
        @Argument long workspaceId, @Argument long connectorId, @Argument String toolName,
        @Argument Map<String, Object> parameters) {

        long userId = currentUserId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, userId, workspaceId);

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        taskToolFacade.setToolParameters(connectorId, toolName, parameters == null ? Map.of() : parameters);

        return true;
    }

    // Ownership check before a mutation: the connector must be one of the caller's added connectors. Returning
    // false (rather than throwing) on a foreign/unknown id keeps the mutation idempotent and prevents id probing.
    private boolean userOwnsConnector(long userId, long workspaceId, long connectorId) {
        return taskToolFacade.listUserComponents(userId, workspaceId)
            .stream()
            .anyMatch(component -> component.getId() == connectorId);
    }

    // Walk the curated TOOLS-typed cluster elements once and group them by component#version, so each added
    // connector can render its tool list without re-scanning the catalog per component.
    private Map<String, List<ToolableClusterElement>> buildToolsByComponent() {
        Map<String, List<ToolableClusterElement>> toolsByComponent = new HashMap<>();

        for (ClusterElementDefinition definition : clusterElementDefinitionService
            .getClusterElementDefinitionStubs(BaseToolFunction.TOOLS)) {

            String key = definition.getComponentName() + "#" + definition.getComponentVersion();

            toolsByComponent
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(new ToolableClusterElement(
                    definition.getName(), definition.getTitle(), definition.getDescription()));
        }

        return toolsByComponent;
    }

    private long currentUserId() {
        return userService.getCurrentUser()
            .getId();
    }

    /**
     * Resolves the binding by tool id, verifying ownership in the process. The lookup walks the user's workspaces'
     * aiHubTasks until a match is found; throws when no owned task contains the id — same shape as a not-found so a
     * probe can't enumerate ids across users.
     */
    private AiHubTaskToolBinding
        findToolBindingOrThrow(long workspaceId, long userId, long taskToolId) {
        // We need the task id to load the binding; rather than introduce a new repository method
        // (find tool → task), iterate the user's recent aiHubTasks in the workspace. For v1 of
        // this controller we use the simpler approach: list-by-task across the user's set. v2 can
        // add a direct lookup if perf shows up.
        for (AiHubTask task : taskService.list(
            workspaceId, userId, /* environment= */ 0, /* status= */ null)) {
            List<AiHubTaskToolBinding> bindings = taskToolFacade.listTaskTools(
                task.getId());

            for (AiHubTaskToolBinding binding : bindings) {
                if (binding.taskToolId() == taskToolId) {
                    return binding;
                }
            }
        }

        throw new IllegalArgumentException(
            "AiHubTaskTool " + taskToolId + " not found in any task owned by user");
    }

    private AiHubTaskToolBinding findToolBindingOrThrow(
        long workspaceId, long userId, long taskToolId, long taskId) {

        // Faster path when we already know the taskId — avoids re-scanning all the user's
        // aiHubTasks after an update.
        AiHubTask task = taskService.getById(taskId, workspaceId, userId);

        return taskToolFacade.listTaskTools(task.getId())
            .stream()
            .filter(binding -> binding.taskToolId() == taskToolId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubTaskTool " + taskToolId + " not found after update"));
    }

    private @Nullable AiHubTaskToolBinding findAnyBindingForComponent(
        long workspaceId, long userId, long taskComponentId) {

        for (AiHubTask task : taskService.list(
            workspaceId, userId, /* environment= */ 0, /* status= */ null)) {
            List<AiHubTaskToolBinding> bindings = taskToolFacade.listTaskTools(
                task.getId());

            for (AiHubTaskToolBinding binding : bindings) {
                if (binding.taskComponentId() == taskComponentId) {
                    return binding;
                }
            }
        }

        return null;
    }

    /**
     * Component-grouped tool catalog. Mutable {@code tools} list because we build it incrementally during the catalog
     * walk; the GraphQL serializer copies it out so external mutation can't reach the cache.
     */
    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record ToolableComponent(
        String componentName, int componentVersion, @Nullable String title, @Nullable String description,
        @Nullable String icon, List<ToolableClusterElement> tools, boolean connectionRequired) {
    }

    public record ToolableClusterElement(
        String name, @Nullable String title, @Nullable String description) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record AttachAiHubTaskToolInput(
        long workspaceId, String taskId, String componentName, @Nullable Integer componentVersion,
        String clusterElementName, @Nullable Long connectionId, int environment,
        @Nullable Map<String, Object> parameters) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record AiHubUserConnector(
        long id, String componentName, int componentVersion, @Nullable String title, @Nullable String description,
        @Nullable String icon, @Nullable Long connectionId, boolean connectionRequired, boolean enabled,
        List<AiHubUserConnectorTool> tools) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record AiHubUserConnectorTool(
        String name, @Nullable String title, @Nullable String description, boolean enabled,
        @Nullable Map<String, ?> parameters) {
    }
}
