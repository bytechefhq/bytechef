/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.chat.repository.AiHubChatComponentRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatConnectorRepository;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatToolRepository;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder.ChatToolReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
public class AiHubChatToolFacadeImpl implements AiHubChatToolFacade {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatToolFacadeImpl.class);

    private final AiHubChatComponentRepository chatComponentRepository;
    private final AiHubChatConnectorRepository chatConnectorRepository;
    private final AiHubChatToolRepository chatToolRepository;
    private final ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatToolFacadeImpl(
        AiHubChatComponentRepository chatComponentRepository,
        AiHubChatConnectorRepository chatConnectorRepository,
        AiHubChatToolRepository chatToolRepository,
        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider) {

        this.chatComponentRepository = chatComponentRepository;
        this.chatConnectorRepository = chatConnectorRepository;
        this.chatToolRepository = chatToolRepository;
        // ObjectProvider so unit tests + deployments without the tool-search advisor configuration still wire the
        // facade cleanly. The per-chat index refresh becomes a no-op in that case; search falls back to the
        // workspace-wide catalog (or, if neither is wired, the LLM has no chat-time tool surface — but the tool
        // ATTACH itself still persists, so the runtime-binding resolver can still synthesize callbacks at turn time).
        this.toolSearchCatalogFeederProvider = toolSearchCatalogFeederProvider;
    }

    @Override
    public long attachComponent(
        long chatId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment) {

        // Idempotency check: identical attach hits the existing row. The unique index would otherwise reject
        // the second insert with a constraint violation; doing the check here makes the contract explicit and
        // returns the existing id rather than asking the caller to handle the violation.
        Optional<AiHubChatComponent> existing = chatComponentRepository.findIdempotencyMatch(
            chatId, componentName, componentVersion, connectionId, environment);

        if (existing.isPresent()) {
            return existing.get()
                .getId();
        }

        AiHubChatComponent component = new AiHubChatComponent(
            chatId, componentName, componentVersion, connectionId, environment);

        component = chatComponentRepository.save(component);

        // Component attach alone doesn't change the searchable tool list (tool rows under the component drive that),
        // but a follow-up addTool will refresh from the joined view. Skipping the refresh here avoids a redundant
        // index rebuild in the common attach-then-addTool sequence.
        return component.getId();
    }

    @Override
    public long addTool(long chatComponentId, String name, Map<String, ?> parameters) {
        // Upsert by (component, name) — same chat-driven idempotency as attachComponent. Re-issuing
        // "configure the sendMessage tool with channel #engineering" updates the existing tool's parameters
        // rather than creating a duplicate row that the LLM would have to disambiguate.
        Optional<AiHubChatTool> existing =
            chatToolRepository.findByChatComponentIdAndName(chatComponentId, name);

        long resultId;

        if (existing.isPresent()) {
            AiHubChatTool tool = existing.get();

            tool.setParameters(parameters);

            tool = chatToolRepository.save(tool);

            resultId = tool.getId();
        } else {
            AiHubChatTool tool =
                new AiHubChatTool(chatComponentId, name, parameters);

            tool = chatToolRepository.save(tool);

            resultId = tool.getId();
        }

        // Parameters change doesn't affect search summaries, but a fresh tool insert does — same refresh code path
        // for both branches keeps the upsert semantics symmetric. Refresh runs after-commit so a failed transaction
        // doesn't leave an indexed entry for a tool that never persisted.
        scheduleRefreshFromComponent(chatComponentId);

        return resultId;
    }

    @Override
    public Optional<Long> findChatComponentIdIgnoringConnection(
        long chatId, String componentName, int componentVersion, int environment) {

        return chatComponentRepository
            .findByChatAndComponentIgnoringConnection(chatId, componentName, componentVersion, environment)
            .map(AiHubChatComponent::getId);
    }

    @Override
    public void setComponentConnection(long chatComponentId, @Nullable Long connectionId) {
        AiHubChatComponent component = chatComponentRepository.findById(chatComponentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubChatComponent " + chatComponentId + " not found"));

        // Idempotent: no-op when the connection is already what the caller wants. Avoids a redundant write
        // (and the per-chat search-index refresh that would follow) on the common "user re-confirmed the same
        // connection" path.
        if (Objects.equals(component.getConnectionId(), connectionId)) {
            return;
        }

        component.setConnectionId(connectionId);

        chatComponentRepository.save(component);

        // Rebinding doesn't change WHICH tools are searchable (component / clusterElement keys are unchanged), but
        // we still refresh so any downstream listener that keys off the binding's connection sees the post-mutation
        // state — same symmetric refresh policy as addTool / updateToolParameters / removeTool.
        scheduleRefreshForChat(component.getChatId());
    }

    @Override
    public void updateToolParameters(long chatToolId, Map<String, ?> parameters) {
        AiHubChatTool tool = chatToolRepository.findById(chatToolId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubChatTool " + chatToolId + " not found"));

        tool.setParameters(parameters);

        chatToolRepository.save(tool);

        // Pre-set parameters don't affect what the LLM finds in search (the search summary comes from the cluster
        // element's description, not the parameters), but we refresh anyway so the sync semantics are simple: any
        // mutation through the facade leaves the search index consistent with the persisted state. Cheap operation
        // — same N tools re-indexed.
        scheduleRefreshFromComponent(tool.getChatComponentId());
    }

    @Override
    public void removeTool(long chatToolId) {
        // Look up the component before delete so we can refresh after-commit using the chat id derived from
        // the parent component. Doing the lookup upfront — rather than after the delete — keeps the refresh path
        // independent of whether a parallel transaction has just removed the row, and avoids a "deleted-row" race
        // where deleteById succeeds but findById returns empty.
        Optional<AiHubChatTool> tool = chatToolRepository.findById(chatToolId);

        // Idempotent: deleting a non-existent id is a no-op so chat affordance "remove the slack tool" can
        // be issued safely without checking existence first.
        chatToolRepository.deleteById(chatToolId);

        tool.ifPresent(value -> scheduleRefreshFromComponent(value.getChatComponentId()));
    }

    @Override
    public void detachComponent(long chatComponentId) {
        // Same look-up-before-delete pattern as removeTool. We need the chat id to refresh the search
        // session, but the component row is about to be cascaded out — fetch it first.
        Optional<AiHubChatComponent> component =
            chatComponentRepository.findById(chatComponentId);

        // FK cascades the dependent AiHubChatTool rows automatically; no manual cleanup needed here.
        chatComponentRepository.deleteById(chatComponentId);

        component.ifPresent(value -> scheduleRefreshForChat(value.getChatId()));
    }

    /**
     * Resolves the chat id for the given component and schedules a per-chat tool-search index refresh. Used by
     * {@link #addTool}, {@link #updateToolParameters}, and {@link #removeTool} where the caller has a
     * {@code chatComponentId} but not the chat id directly. A missing component (deleted between the mutation and this
     * lookup) is logged and ignored — the index becomes stale by one mutation cycle, which is acceptable since the next
     * mutation through the facade will re-converge it.
     */
    private void scheduleRefreshFromComponent(long chatComponentId) {
        Optional<AiHubChatComponent> component =
            chatComponentRepository.findById(chatComponentId);

        if (component.isEmpty()) {
            return;
        }

        scheduleRefreshForChat(component.get()
            .getChatId());
    }

    /**
     * Schedules an after-commit refresh of the chat's tool-search subset. Pulls the current full binding list from the
     * joined view {@link #listChatTools(long)} after the transaction commits, so the populate sees the post-mutation
     * state. Running afterCommit (not in-transaction) means: if the caller's transaction rolls back, the index never
     * picks up the would-be mutation; if it commits, the index sees what the next chat turn would observe.
     *
     * <p>
     * No-op when the feeder bean isn't wired (deployments without the tool-search advisor). Failures during the
     * deferred populate are logged at WARN — the user-visible mutation has already succeeded, and the index becomes
     * eventually-consistent on the next mutation that DOES succeed.
     * </p>
     */
    private void scheduleRefreshForChat(@Nullable Long chatId) {
        // User-global connectors (added on the Connectors page) carry a null chatId: they belong to a
        // (userId, workspaceId) scope, not a single chat, so there is no per-chat search subset to refresh.
        // Skipping here keeps detachComponent/removeTool idempotent for both chat-bound and user-global rows.
        if (chatId == null) {
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
                    refreshIndex(feeder, chatId);
                }
            });
        } else {
            refreshIndex(feeder, chatId);
        }
    }

    private void refreshIndex(ToolSearchCatalogFeeder feeder, long chatId) {
        try {
            List<AiHubChatToolBinding> bindings = listChatTools(chatId);

            // Map join-shape bindings to the lighter ChatToolReference triple the feeder consumes. Drops
            // connection / parameters since the tool-search index keys off (component, version, clusterElement).
            List<ChatToolReference> references = bindings.stream()
                .map(binding -> new ChatToolReference(
                    binding.componentName(), binding.componentVersion(), binding.clusterElementName()))
                .toList();

            feeder.populateForChat(chatId, references);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to refresh per-chat tool search index for chat {} after tool mutation; "
                    + "next mutation will re-converge.",
                chatId, exception);
        }
    }

    @Override
    public List<AiHubChatToolBinding> listChatTools(long chatId) {
        List<AiHubChatComponent> components = chatComponentRepository.findAllByChatId(
            chatId);

        if (components.isEmpty()) {
            return List.of();
        }

        List<AiHubChatToolBinding> bindings = new ArrayList<>();

        for (AiHubChatComponent component : components) {
            List<AiHubChatTool> tools = chatToolRepository.findAllByChatComponentId(
                component.getId());

            for (AiHubChatTool tool : tools) {
                bindings.add(
                    new AiHubChatToolBinding(
                        tool.getId(), component.getId(), chatId, component.getComponentName(),
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

        Optional<AiHubChatComponent> existing =
            chatComponentRepository.findByUserIdAndWorkspaceIdAndComponentNameAndComponentVersion(
                userId, workspaceId, componentName, componentVersion);

        if (existing.isPresent()) {
            AiHubChatComponent component = existing.get();

            // Adding an already-added connector refreshes its connection in place (the add dialog can pick a
            // different one) rather than creating a duplicate — the partial unique index would reject that anyway.
            if (!Objects.equals(component.getConnectionId(), connectionId)) {
                component.setConnectionId(connectionId);

                chatComponentRepository.save(component);
            }

            return component.getId();
        }

        AiHubChatComponent component = new AiHubChatComponent();

        component.setUserId(userId);
        component.setWorkspaceId(workspaceId);
        component.setComponentName(componentName);
        component.setComponentVersion(componentVersion);
        component.setConnectionId(connectionId);
        component.setEnvironment(environment);
        // chatId stays null => user-global "added connector".

        return chatComponentRepository.save(component)
            .getId();
    }

    @Override
    public List<AiHubChatComponent> listUserComponents(long userId, long workspaceId) {
        return chatComponentRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    public List<AiHubChatTool> listComponentTools(long chatComponentId) {
        return chatToolRepository.findAllByChatComponentId(chatComponentId);
    }

    @Override
    public List<AiHubChatToolBinding> listUserTools(long userId, long workspaceId) {
        List<AiHubChatComponent> components =
            chatComponentRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        if (components.isEmpty()) {
            return List.of();
        }

        List<AiHubChatToolBinding> bindings = new ArrayList<>();

        for (AiHubChatComponent component : components) {
            if (!component.isEnabled()) {
                continue;
            }

            for (AiHubChatTool tool : chatToolRepository.findAllByChatComponentId(component.getId())) {
                if (!tool.isEnabled()) {
                    continue;
                }

                bindings.add(
                    new AiHubChatToolBinding(
                        tool.getId(), component.getId(), 0L, component.getComponentName(),
                        component.getComponentVersion(), tool.getName(), component.getConnectionId(),
                        component.getEnvironment(), tool.getParameters()));
            }
        }

        return bindings;
    }

    @Override
    public void setComponentEnabled(long chatComponentId, boolean enabled) {
        AiHubChatComponent component = chatComponentRepository.findById(chatComponentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubChatComponent " + chatComponentId + " not found"));

        component.setEnabled(enabled);

        chatComponentRepository.save(component);
    }

    @Override
    public void setChatConnectorEnabled(long chatId, long userConnectorId, boolean enabled) {
        AiHubChatComponent userComponent = chatComponentRepository.findById(userConnectorId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubChatComponent " + userConnectorId + " not found"));

        String componentName = userComponent.getComponentName();

        // Participation rides its own table rather than a chat-scoped ai_hub_chat_component row: see
        // AiHubChatConnector's class javadoc for why reusing that row is wrong rather than merely redundant.
        // A row is materialized on the first toggle — until then, absence means participating.
        AiHubChatConnector chatConnector = chatConnectorRepository.findByChatIdAndComponentName(chatId, componentName)
            .orElseGet(() -> new AiHubChatConnector(chatId, componentName, enabled));

        chatConnector.setEnabled(enabled);

        chatConnectorRepository.save(chatConnector);
    }

    @Override
    public Set<String> listChatDisabledConnectors(long chatId) {
        Set<String> disabled = new HashSet<>();

        for (AiHubChatConnector connector : chatConnectorRepository.findAllByChatId(chatId)) {
            if (!connector.isEnabled()) {
                disabled.add(connector.getComponentName());
            }
        }

        return disabled;
    }

    @Override
    public void setToolEnabled(long chatComponentId, String toolName, boolean enabled) {
        // Upsert the tool row carrying the enabled flag. A missing row means "enabled" (the default), so
        // disabling persists a row and re-enabling flips it back, preserving any configured parameters.
        AiHubChatTool tool = chatToolRepository.findByChatComponentIdAndName(chatComponentId, toolName)
            .orElseGet(() -> new AiHubChatTool(chatComponentId, toolName, Map.of()));

        tool.setEnabled(enabled);

        chatToolRepository.save(tool);
    }

    @Override
    public void setToolParameters(long chatComponentId, String toolName, Map<String, ?> parameters) {
        // Same upsert-by-(component, name) shape as setToolEnabled. A missing row defaults to enabled, so
        // configuring parameters on a not-yet-toggled tool keeps it enabled while persisting its parameters.
        AiHubChatTool tool = chatToolRepository.findByChatComponentIdAndName(chatComponentId, toolName)
            .orElseGet(() -> new AiHubChatTool(chatComponentId, toolName, Map.of()));

        tool.setParameters(parameters);

        chatToolRepository.save(tool);
    }
}
