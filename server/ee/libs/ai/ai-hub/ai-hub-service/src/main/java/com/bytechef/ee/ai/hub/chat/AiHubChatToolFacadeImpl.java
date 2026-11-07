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
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnAiHub
public class AiHubChatToolFacadeImpl implements AiHubChatToolFacade {

    private final AiHubChatComponentRepository chatComponentRepository;
    private final AiHubChatConnectorRepository chatConnectorRepository;
    private final AiHubChatToolRepository chatToolRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatToolFacadeImpl(
        AiHubChatComponentRepository chatComponentRepository,
        AiHubChatConnectorRepository chatConnectorRepository,
        AiHubChatToolRepository chatToolRepository) {

        this.chatComponentRepository = chatComponentRepository;
        this.chatConnectorRepository = chatConnectorRepository;
        this.chatToolRepository = chatToolRepository;
    }

    @Override
    public long attachComponent(
        long chatId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment) {

        Optional<AiHubChatComponent> existing = chatComponentRepository.findIdempotencyMatch(
            chatId, componentName, componentVersion, connectionId, environment);

        if (existing.isPresent()) {
            return existing.get()
                .getId();
        }

        AiHubChatComponent component = new AiHubChatComponent(
            chatId, componentName, componentVersion, connectionId, environment);

        component = chatComponentRepository.save(component);

        return component.getId();
    }

    @Override
    public long addTool(long chatComponentId, String name, Map<String, ?> parameters) {
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

        if (Objects.equals(component.getConnectionId(), connectionId)) {
            return;
        }

        component.setConnectionId(connectionId);

        chatComponentRepository.save(component);
    }

    @Override
    public void updateToolParameters(long chatToolId, Map<String, ?> parameters) {
        AiHubChatTool tool = chatToolRepository.findById(chatToolId)
            .orElseThrow(() -> new IllegalArgumentException(
                "AiHubChatTool " + chatToolId + " not found"));

        tool.setParameters(parameters);

        chatToolRepository.save(tool);
    }

    @Override
    public void removeTool(long chatToolId) {
        chatToolRepository.deleteById(chatToolId);
    }

    @Override
    public void detachComponent(long chatComponentId) {
        chatComponentRepository.deleteById(chatComponentId);
    }

    @Override
    public void deleteChatBindings(long chatId) {
        for (AiHubChatComponent chatComponent : chatComponentRepository.findAllByChatId(chatId)) {
            chatToolRepository.deleteAll(chatToolRepository.findAllByChatComponentId(chatComponent.getId()));

            chatComponentRepository.delete(chatComponent);
        }

        chatConnectorRepository.deleteAll(chatConnectorRepository.findAllByChatId(chatId));
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
                        component.getEnvironment(), tool.getParameters(), tool.isRequiresApproval()));
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
                        component.getEnvironment(), tool.getParameters(), tool.isRequiresApproval()));
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
        AiHubChatTool tool = findOrCreateTool(chatComponentId, toolName);

        tool.setEnabled(enabled);

        chatToolRepository.save(tool);
    }

    @Override
    public void setToolParameters(long chatComponentId, String toolName, Map<String, ?> parameters) {
        AiHubChatTool tool = findOrCreateTool(chatComponentId, toolName);

        tool.setParameters(parameters);

        chatToolRepository.save(tool);
    }

    @Override
    public void setToolRequiresApproval(long chatToolId, boolean requiresApproval) {
        AiHubChatTool tool = chatToolRepository.findById(chatToolId)
            .orElseThrow(() -> new NotFoundException("AiHubChatTool", chatToolId));

        tool.setRequiresApproval(requiresApproval);

        chatToolRepository.save(tool);
    }

    @Override
    public void setToolRequiresApproval(long chatComponentId, String toolName, boolean requiresApproval) {
        AiHubChatTool tool = findOrCreateTool(chatComponentId, toolName);

        tool.setRequiresApproval(requiresApproval);

        chatToolRepository.save(tool);
    }

    private AiHubChatTool findOrCreateTool(long chatComponentId, String toolName) {
        return chatToolRepository.findByChatComponentIdAndName(chatComponentId, toolName)
            .orElseGet(() -> new AiHubChatTool(chatComponentId, toolName, Map.of()));
    }
}
