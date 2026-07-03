/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.ee.ai.hub.task.repository.AiHubTaskComponentRepository;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskToolRepository;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder.TaskToolReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiHubTaskToolFacadeImpl implements AiHubTaskToolFacade {

    private static final Logger log = LoggerFactory.getLogger(AiHubTaskToolFacadeImpl.class);

    private final AiHubTaskComponentRepository taskComponentRepository;
    private final AiHubTaskToolRepository taskToolRepository;
    private final ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubTaskToolFacadeImpl(
        AiHubTaskComponentRepository taskComponentRepository,
        AiHubTaskToolRepository taskToolRepository,
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider) {

        this.taskComponentRepository = taskComponentRepository;
        this.taskToolRepository = taskToolRepository;
        // ObjectProvider so unit tests + deployments without the tool-search advisor configuration still wire the
        // facade cleanly. The per-task index refresh becomes a no-op in that case; search falls back to the
        // workspace-wide catalog (or, if neither is wired, the LLM has no chat-time tool surface — but the tool
        // ATTACH itself still persists, so the runtime-binding resolver can still synthesize callbacks at turn time).
        this.toolSearchCatalogFeederProvider = toolSearchCatalogFeederProvider;
    }

    @Override
    public long attachComponent(
        long taskId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment) {

        // Idempotency check: identical attach hits the existing row. The unique index would otherwise reject
        // the second insert with a constraint violation; doing the check here makes the contract explicit and
        // returns the existing id rather than asking the caller to handle the violation.
        Optional<AiHubTaskComponent> existing = taskComponentRepository.findIdempotencyMatch(
            taskId, componentName, componentVersion, connectionId, environment);

        if (existing.isPresent()) {
            return existing.get()
                .getId();
        }

        AiHubTaskComponent component = new AiHubTaskComponent(
            taskId, componentName, componentVersion, connectionId, environment);

        component = taskComponentRepository.save(component);

        // Component attach alone doesn't change the searchable tool list (tool rows under the component drive that),
        // but a follow-up addTool will refresh from the joined view. Skipping the refresh here avoids a redundant
        // index rebuild in the common attach-then-addTool sequence.
        return component.getId();
    }

    @Override
    public long addTool(long taskComponentId, String name, Map<String, ?> parameters) {
        // Upsert by (component, name) — same chat-driven idempotency as attachComponent. Re-issuing
        // "configure the sendMessage tool with channel #engineering" updates the existing tool's parameters
        // rather than creating a duplicate row that the LLM would have to disambiguate.
        Optional<AiHubTaskTool> existing =
            taskToolRepository.findByTaskComponentIdAndName(taskComponentId, name);

        long resultId;

        if (existing.isPresent()) {
            AiHubTaskTool tool = existing.get();

            tool.setParameters(parameters);

            tool = taskToolRepository.save(tool);

            resultId = tool.getId();
        } else {
            AiHubTaskTool tool =
                new AiHubTaskTool(taskComponentId, name, parameters);

            tool = taskToolRepository.save(tool);

            resultId = tool.getId();
        }

        // Parameters change doesn't affect search summaries, but a fresh tool insert does — same refresh code path
        // for both branches keeps the upsert semantics symmetric. Refresh runs after-commit so a failed transaction
        // doesn't leave an indexed entry for a tool that never persisted.
        scheduleRefreshFromComponent(taskComponentId);

        return resultId;
    }

    @Override
    public Optional<Long> findTaskComponentIdIgnoringConnection(
        long taskId, String componentName, int componentVersion, int environment) {

        return taskComponentRepository
            .findByTaskAndComponentIgnoringConnection(taskId, componentName, componentVersion, environment)
            .map(AiHubTaskComponent::getId);
    }

    @Override
    public void setComponentConnection(long taskComponentId, @Nullable Long connectionId) {
        AiHubTaskComponent component = taskComponentRepository.findById(taskComponentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubTaskComponent " + taskComponentId + " not found"));

        // Idempotent: no-op when the connection is already what the caller wants. Avoids a redundant write
        // (and the per-task search-index refresh that would follow) on the common "user re-confirmed the same
        // connection" path.
        if (Objects.equals(component.getConnectionId(), connectionId)) {
            return;
        }

        component.setConnectionId(connectionId);

        taskComponentRepository.save(component);

        // Rebinding doesn't change WHICH tools are searchable (component / clusterElement keys are unchanged), but
        // we still refresh so any downstream listener that keys off the binding's connection sees the post-mutation
        // state — same symmetric refresh policy as addTool / updateToolParameters / removeTool.
        scheduleRefreshForTask(component.getTaskId());
    }

    @Override
    public void updateToolParameters(long taskToolId, Map<String, ?> parameters) {
        AiHubTaskTool tool = taskToolRepository.findById(taskToolId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubTaskTool " + taskToolId + " not found"));

        tool.setParameters(parameters);

        taskToolRepository.save(tool);

        // Pre-set parameters don't affect what the LLM finds in search (the search summary comes from the cluster
        // element's description, not the parameters), but we refresh anyway so the sync semantics are simple: any
        // mutation through the facade leaves the search index consistent with the persisted state. Cheap operation
        // — same N tools re-indexed.
        scheduleRefreshFromComponent(tool.getTaskComponentId());
    }

    @Override
    public void removeTool(long taskToolId) {
        // Look up the component before delete so we can refresh after-commit using the task id derived from
        // the parent component. Doing the lookup upfront — rather than after the delete — keeps the refresh path
        // independent of whether a parallel transaction has just removed the row, and avoids a "deleted-row" race
        // where deleteById succeeds but findById returns empty.
        Optional<AiHubTaskTool> tool = taskToolRepository.findById(taskToolId);

        // Idempotent: deleting a non-existent id is a no-op so chat affordance "remove the slack tool" can
        // be issued safely without checking existence first.
        taskToolRepository.deleteById(taskToolId);

        tool.ifPresent(value -> scheduleRefreshFromComponent(value.getTaskComponentId()));
    }

    @Override
    public void detachComponent(long taskComponentId) {
        // Same look-up-before-delete pattern as removeTool. We need the task id to refresh the search
        // session, but the component row is about to be cascaded out — fetch it first.
        Optional<AiHubTaskComponent> component =
            taskComponentRepository.findById(taskComponentId);

        // FK cascades the dependent AiHubTaskTool rows automatically; no manual cleanup needed here.
        taskComponentRepository.deleteById(taskComponentId);

        component.ifPresent(value -> scheduleRefreshForTask(value.getTaskId()));
    }

    /**
     * Resolves the task id for the given component and schedules a per-task tool-search index refresh. Used by
     * {@link #addTool}, {@link #updateToolParameters}, and {@link #removeTool} where the caller has a
     * {@code taskComponentId} but not the task id directly. A missing component (deleted between the mutation and this
     * lookup) is logged and ignored — the index becomes stale by one mutation cycle, which is acceptable since the next
     * mutation through the facade will re-converge it.
     */
    private void scheduleRefreshFromComponent(long taskComponentId) {
        Optional<AiHubTaskComponent> component =
            taskComponentRepository.findById(taskComponentId);

        if (component.isEmpty()) {
            return;
        }

        scheduleRefreshForTask(component.get()
            .getTaskId());
    }

    /**
     * Schedules an after-commit refresh of the task's tool-search subset. Pulls the current full binding list from the
     * joined view {@link #listTaskTools(long)} after the transaction commits, so the populate sees the post-mutation
     * state. Running afterCommit (not in-transaction) means: if the caller's transaction rolls back, the index never
     * picks up the would-be mutation; if it commits, the index sees what the next chat turn would observe.
     *
     * <p>
     * No-op when the feeder bean isn't wired (deployments without the tool-search advisor). Failures during the
     * deferred populate are logged at WARN — the user-visible mutation has already succeeded, and the index becomes
     * eventually-consistent on the next mutation that DOES succeed.
     * </p>
     */
    private void scheduleRefreshForTask(@Nullable Long taskId) {
        // User-global connectors (added on the Connectors page) carry a null taskId: they belong to a
        // (userId, workspaceId) scope, not a single task, so there is no per-task search subset to refresh.
        // Skipping here keeps detachComponent/removeTool idempotent for both task-bound and user-global rows.
        if (taskId == null) {
            return;
        }

        if (toolSearchCatalogFeederProvider == null) {
            return;
        }

        ToolSearchCatalogFeeder feeder = toolSearchCatalogFeederProvider.getIfAvailable();

        if (feeder == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    refreshIndex(feeder, taskId);
                }
            });
        } else {
            refreshIndex(feeder, taskId);
        }
    }

    private void refreshIndex(ToolSearchCatalogFeeder feeder, long taskId) {
        try {
            List<AiHubTaskToolBinding> bindings = listTaskTools(taskId);

            // Map join-shape bindings to the lighter TaskToolReference triple the feeder consumes. Drops
            // connection / parameters since the tool-search index keys off (component, version, clusterElement).
            List<TaskToolReference> references = bindings.stream()
                .map(binding -> new TaskToolReference(
                    binding.componentName(), binding.componentVersion(), binding.clusterElementName()))
                .toList();

            feeder.populateForTask(taskId, references);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to refresh per-task tool search index for task {} after tool mutation; "
                    + "next mutation will re-converge.",
                taskId, exception);
        }
    }

    @Override
    public List<AiHubTaskToolBinding> listTaskTools(long taskId) {
        List<AiHubTaskComponent> components = taskComponentRepository.findAllByTaskId(
            taskId);

        if (components.isEmpty()) {
            return List.of();
        }

        List<AiHubTaskToolBinding> bindings = new ArrayList<>();

        for (AiHubTaskComponent component : components) {
            List<AiHubTaskTool> tools = taskToolRepository.findAllByTaskComponentId(
                component.getId());

            for (AiHubTaskTool tool : tools) {
                bindings.add(
                    new AiHubTaskToolBinding(
                        tool.getId(), component.getId(), taskId, component.getComponentName(),
                        component.getComponentVersion(), tool.getName(), component.getConnectionId(),
                        component.getEnvironment(), tool.getParameters()));
            }
        }

        return bindings;
    }

    @Override
    public long attachUserComponent(
        long userId, long workspaceId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment) {

        Optional<AiHubTaskComponent> existing =
            taskComponentRepository.findByUserIdAndWorkspaceIdAndComponentNameAndComponentVersion(
                userId, workspaceId, componentName, componentVersion);

        if (existing.isPresent()) {
            AiHubTaskComponent component = existing.get();

            // Adding an already-added connector refreshes its connection in place (the add dialog can pick a
            // different one) rather than creating a duplicate — the partial unique index would reject that anyway.
            if (!Objects.equals(component.getConnectionId(), connectionId)) {
                component.setConnectionId(connectionId);

                taskComponentRepository.save(component);
            }

            return component.getId();
        }

        AiHubTaskComponent component = new AiHubTaskComponent();

        component.setUserId(userId);
        component.setWorkspaceId(workspaceId);
        component.setComponentName(componentName);
        component.setComponentVersion(componentVersion);
        component.setConnectionId(connectionId);
        component.setEnvironment(environment);
        // taskId stays null => user-global "added connector".

        return taskComponentRepository.save(component)
            .getId();
    }

    @Override
    public List<AiHubTaskComponent> listUserComponents(long userId, long workspaceId) {
        return taskComponentRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    public List<AiHubTaskTool> listComponentTools(long taskComponentId) {
        return taskToolRepository.findAllByTaskComponentId(taskComponentId);
    }

    @Override
    public List<AiHubTaskToolBinding> listUserTools(long userId, long workspaceId) {
        List<AiHubTaskComponent> components =
            taskComponentRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        if (components.isEmpty()) {
            return List.of();
        }

        List<AiHubTaskToolBinding> bindings = new ArrayList<>();

        for (AiHubTaskComponent component : components) {
            if (!component.isEnabled()) {
                continue;
            }

            for (AiHubTaskTool tool : taskToolRepository.findAllByTaskComponentId(component.getId())) {
                if (!tool.isEnabled()) {
                    continue;
                }

                bindings.add(
                    new AiHubTaskToolBinding(
                        tool.getId(), component.getId(), 0L, component.getComponentName(),
                        component.getComponentVersion(), tool.getName(), component.getConnectionId(),
                        component.getEnvironment(), tool.getParameters()));
            }
        }

        return bindings;
    }

    @Override
    public void setComponentEnabled(long taskComponentId, boolean enabled) {
        AiHubTaskComponent component = taskComponentRepository.findById(taskComponentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubTaskComponent " + taskComponentId + " not found"));

        component.setEnabled(enabled);

        taskComponentRepository.save(component);
    }

    @Override
    public void setToolEnabled(long taskComponentId, String toolName, boolean enabled) {
        // Upsert the tool row carrying the enabled flag. A missing row means "enabled" (the default), so
        // disabling persists a row and re-enabling flips it back, preserving any configured parameters.
        AiHubTaskTool tool = taskToolRepository.findByTaskComponentIdAndName(taskComponentId, toolName)
            .orElseGet(() -> new AiHubTaskTool(taskComponentId, toolName, Map.of()));

        tool.setEnabled(enabled);

        taskToolRepository.save(tool);
    }

    @Override
    public void setToolParameters(long taskComponentId, String toolName, Map<String, ?> parameters) {
        // Same upsert-by-(component, name) shape as setToolEnabled. A missing row defaults to enabled, so
        // configuring parameters on a not-yet-toggled tool keeps it enabled while persisting its parameters.
        AiHubTaskTool tool = taskToolRepository.findByTaskComponentIdAndName(taskComponentId, toolName)
            .orElseGet(() -> new AiHubTaskTool(taskComponentId, toolName, Map.of()));

        tool.setParameters(parameters);

        taskToolRepository.save(tool);
    }
}
