/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.graphql;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatComponent;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatTool;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolConnectionValidator;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
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
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnAiHub
public class AiHubChatToolGraphQlController {

    private static final int DEFAULT_COMPONENT_VERSION = 1;

    private final AiHubChatService chatService;
    private final AiHubChatToolFacade chatToolFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final AiHubChatToolConnectionValidator connectionValidator;
    private final PermissionService permissionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public AiHubChatToolGraphQlController(
        AiHubChatService chatService, AiHubChatToolFacade chatToolFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, AiHubChatToolConnectionValidator connectionValidator,
        PermissionService permissionService, UserService userService) {

        this.chatService = chatService;
        this.chatToolFacade = chatToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionValidator = connectionValidator;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @QueryMapping
    public List<AiHubChatToolBinding> aiHubChatTools(
        @Argument long workspaceId, @Argument String chatId) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        return chatService.findByThreadIdViewable(chatId, userId)
            .filter(chat -> chatService.getWorkspaceId(chat.getId()) == workspaceId)
            .map(chat -> chatToolFacade.listChatTools(chat.getId()))
            .orElseGet(List::of);
    }

    @QueryMapping
    public List<ToolableComponent> aiHubChatToolableComponents(@Argument long workspaceId) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        List<ClusterElementDefinition> toolDefinitions =
            clusterElementDefinitionService.getClusterElementDefinitionStubs(BaseToolFunction.TOOLS);

        Map<String, ToolableComponent> byComponent = new HashMap<>();
        Map<String, ComponentDefinition> componentCache = new HashMap<>();

        for (ClusterElementDefinition definition : toolDefinitions) {
            String key = definition.getComponentName() + "#" + definition.getComponentVersion();

            ComponentDefinition componentDefinition = componentCache.computeIfAbsent(
                key,
                componentKey -> componentDefinitionService.getComponentDefinition(
                    definition.getComponentName(), definition.getComponentVersion()));

            if (componentDefinition.isClusterRoot() || componentDefinition.getConnection() == null) {
                continue;
            }

            ToolableComponent component = byComponent.computeIfAbsent(
                key,
                componentKey -> new ToolableComponent(
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
    public AiHubChatToolBinding attachAiHubChatTool(@Argument AttachAiHubChatToolInput input) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, input.workspaceId());

        long userId = currentUserId();

        AiHubChat chat = resolveOwnedChat(input.chatId(), input.workspaceId(), userId)
            .orElseThrow(() -> new NotFoundException("AiHubChat", input.chatId()));

        Integer inputComponentVersion = input.componentVersion();

        int componentVersion = inputComponentVersion == null ? DEFAULT_COMPONENT_VERSION : inputComponentVersion;
        int environment = (int) chat.getEnvironmentId();

        Long connectionId = input.connectionId();

        if (connectionId != null) {
            connectionValidator.validate(input.workspaceId(), input.componentName(), connectionId, environment);
        }

        long chatComponentId = chatToolFacade.attachComponent(
            chat.getId(), input.componentName(), componentVersion, connectionId, environment);

        Map<String, ?> parameters = input.parameters() == null ? Map.of() : input.parameters();

        long chatToolId = chatToolFacade.addTool(
            chatComponentId, input.clusterElementName(), parameters);

        return chatToolFacade.listChatTools(chat.getId())
            .stream()
            .filter(binding -> binding.chatToolId() == chatToolId)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Just-attached tool " + chatToolId + " not found in chat listing"));
    }

    @MutationMapping
    public AiHubChatToolBinding updateAiHubChatToolParameters(
        @Argument long workspaceId, @Argument long chatToolId, @Argument Map<String, Object> parameters) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        findOwnedToolBinding(workspaceId, userId, chatToolId);

        chatToolFacade.updateToolParameters(chatToolId, parameters);

        return findOwnedToolBinding(workspaceId, userId, chatToolId);
    }

    @MutationMapping
    public boolean removeAiHubChatTool(@Argument long workspaceId, @Argument long chatToolId) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        AiHubChatToolBinding existing = findOwnedToolBinding(workspaceId, userId, chatToolId);

        chatToolFacade.removeTool(existing.chatToolId());

        return true;
    }

    @MutationMapping
    public AiHubChatToolBinding setAiHubChatToolRequiresApproval(
        @Argument long workspaceId, @Argument long chatToolId, @Argument boolean requiresApproval) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        AiHubChatToolBinding existing = findOwnedToolBinding(workspaceId, userId, chatToolId);

        chatToolFacade.setToolRequiresApproval(existing.chatToolId(), requiresApproval);

        return findOwnedToolBinding(workspaceId, userId, chatToolId);
    }

    @MutationMapping
    public boolean detachAiHubChatComponent(
        @Argument long workspaceId, @Argument long chatComponentId) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsChatComponent(workspaceId, userId, chatComponentId)) {
            return false;
        }

        chatToolFacade.detachComponent(chatComponentId);

        return true;
    }

    @QueryMapping
    public List<AiHubUserConnector> aiHubUserConnectors(
        @Argument long workspaceId, @Argument @Nullable String chatId) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        List<AiHubChatComponent> components = chatToolFacade.listUserComponents(userId, workspaceId);

        if (components.isEmpty()) {
            return List.of();
        }

        Set<String> disabledInChat = resolveOwnedChat(chatId, workspaceId, userId)
            .map(chat -> chatToolFacade.listChatDisabledConnectors(chat.getId()))
            .orElseGet(Set::of);

        Map<String, List<ToolableClusterElement>> toolsByComponent = buildToolsByComponent();
        Map<String, ComponentDefinition> componentCache = new HashMap<>();

        List<AiHubUserConnector> connectors = new ArrayList<>();

        for (AiHubChatComponent component : components) {
            String key = component.getComponentName() + "#" + component.getComponentVersion();

            ComponentDefinition componentDefinition = componentCache.computeIfAbsent(
                key,
                componentKey -> componentDefinitionService.getComponentDefinition(
                    component.getComponentName(), component.getComponentVersion()));

            Set<String> disabledTools = new HashSet<>();
            Set<String> requiresApprovalTools = new HashSet<>();
            Map<String, Map<String, ?>> parametersByTool = new HashMap<>();

            for (AiHubChatTool tool : chatToolFacade.listComponentTools(component.getId())) {
                if (!tool.isEnabled()) {
                    disabledTools.add(tool.getName());
                }

                if (tool.isRequiresApproval()) {
                    requiresApprovalTools.add(tool.getName());
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
                    !disabledTools.contains(catalogTool.name()), parametersByTool.get(catalogTool.name()),
                    requiresApprovalTools.contains(catalogTool.name())))
                .toList();

            connectors.add(
                new AiHubUserConnector(
                    component.getId(), component.getComponentName(), component.getComponentVersion(),
                    componentDefinition.getTitle(), componentDefinition.getDescription(),
                    componentDefinition.getIcon(), component.getConnectionId(),
                    componentDefinition.isConnectionRequired(), component.isEnabled(),
                    !disabledInChat.contains(component.getComponentName()), tools));
        }

        return connectors;
    }

    @MutationMapping
    public String addAiHubUserConnector(
        @Argument long workspaceId, @Argument String componentName, @Argument int componentVersion,
        @Argument @Nullable Long connectionId, @Argument int environment) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (connectionId != null) {
            connectionValidator.validate(workspaceId, componentName, connectionId, environment);
        }

        long connectorId = chatToolFacade.attachUserComponent(
            userId, workspaceId, componentName, componentVersion, connectionId, environment);

        return String.valueOf(connectorId);
    }

    @MutationMapping
    public boolean removeAiHubUserConnector(@Argument long workspaceId, @Argument long connectorId) {
        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        chatToolFacade.detachComponent(connectorId);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorEnabled(
        @Argument long workspaceId, @Argument long connectorId, @Argument boolean enabled) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        chatToolFacade.setComponentEnabled(connectorId, enabled);

        return true;
    }

    @MutationMapping
    public boolean setAiHubChatConnectorEnabled(
        @Argument long workspaceId, @Argument String chatId, @Argument long connectorId,
        @Argument boolean enabled) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        Optional<AiHubChat> chat = resolveOwnedChat(chatId, workspaceId, userId);

        if (chat.isEmpty()) {
            return false;
        }

        chatToolFacade.setChatConnectorEnabled(
            chat.get()
                .getId(),
            connectorId, enabled);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorToolEnabled(
        @Argument long workspaceId, @Argument long connectorId, @Argument String toolName,
        @Argument boolean enabled) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        chatToolFacade.setToolEnabled(connectorId, toolName, enabled);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorToolParameters(
        @Argument long workspaceId, @Argument long connectorId, @Argument String toolName,
        @Argument Map<String, Object> parameters) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        chatToolFacade.setToolParameters(connectorId, toolName, parameters == null ? Map.of() : parameters);

        return true;
    }

    @MutationMapping
    public boolean setAiHubUserConnectorToolRequiresApproval(
        @Argument long workspaceId, @Argument long connectorId, @Argument String toolName,
        @Argument boolean requiresApproval) {

        AiHubWorkspaceAccess.verifyViewer(permissionService, workspaceId);

        long userId = currentUserId();

        if (!userOwnsConnector(userId, workspaceId, connectorId)) {
            return false;
        }

        chatToolFacade.setToolRequiresApproval(connectorId, toolName, requiresApproval);

        return true;
    }

    private Optional<AiHubChat> resolveOwnedChat(@Nullable String chatId, long workspaceId, long userId) {
        if (chatId == null) {
            return Optional.empty();
        }

        return chatService.findNotDeletedByThreadId(chatId)
            .filter(chat -> chat.getUserId() == userId && chatService.getWorkspaceId(chat.getId()) == workspaceId);
    }

    private boolean userOwnsConnector(long userId, long workspaceId, long connectorId) {
        return chatToolFacade.listUserComponents(userId, workspaceId)
            .stream()
            .anyMatch(component -> component.getId() == connectorId);
    }

    private Map<String, List<ToolableClusterElement>> buildToolsByComponent() {
        Map<String, List<ToolableClusterElement>> toolsByComponent = new HashMap<>();

        for (ClusterElementDefinition definition : clusterElementDefinitionService
            .getClusterElementDefinitionStubs(BaseToolFunction.TOOLS)) {

            String key = definition.getComponentName() + "#" + definition.getComponentVersion();

            toolsByComponent
                .computeIfAbsent(key, componentKey -> new ArrayList<>())
                .add(new ToolableClusterElement(
                    definition.getName(), definition.getTitle(), definition.getDescription()));
        }

        return toolsByComponent;
    }

    private long currentUserId() {
        return userService.getCurrentUser()
            .getId();
    }

    private AiHubChatToolBinding findOwnedToolBinding(long workspaceId, long userId, long chatToolId) {
        AiHubChatToolBinding binding = chatToolFacade.fetchChatTool(chatToolId)
            .orElseThrow(() -> new NotFoundException("AiHubChatTool", chatToolId));

        if (!isOwnedChat(binding.chatId(), workspaceId, userId)) {
            throw new ForbiddenException("AiHubChatTool " + chatToolId + " does not belong to a chat you own");
        }

        return binding;
    }

    private boolean userOwnsChatComponent(long workspaceId, long userId, long chatComponentId) {
        return chatToolFacade.fetchChatComponent(chatComponentId)
            .map(AiHubChatComponent::getChatId)
            .filter(chatId -> isOwnedChat(chatId, workspaceId, userId))
            .isPresent();
    }

    private boolean isOwnedChat(long chatId, long workspaceId, long userId) {
        if (chatId <= 0) {
            return false;
        }

        try {
            AiHubChat chat = chatService.getById(chatId, workspaceId, userId);

            return chat.getUserId() == userId;
        } catch (NotFoundException | ForbiddenException exception) {
            return false;
        }
    }

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
    public record AttachAiHubChatToolInput(
        long workspaceId, String chatId, String componentName, @Nullable Integer componentVersion,
        String clusterElementName, @Nullable Long connectionId, int environment,
        @Nullable Map<String, Object> parameters) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record AiHubUserConnector(
        long id, String componentName, int componentVersion, @Nullable String title, @Nullable String description,
        @Nullable String icon, @Nullable Long connectionId, boolean connectionRequired, boolean enabled,
        boolean enabledInChat, List<AiHubUserConnectorTool> tools) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public record AiHubUserConnectorTool(
        String name, @Nullable String title, @Nullable String description, boolean enabled,
        @Nullable Map<String, ?> parameters, boolean requiresApproval) {
    }
}
