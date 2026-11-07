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
 * @version ee
 * @author Ivica Cardic
 */
public interface AiHubChatToolFacade {

    long attachComponent(
        long chatId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment);

    long addTool(long chatComponentId, String name, Map<String, ?> parameters);

    void updateToolParameters(long chatToolId, Map<String, ?> parameters);

    void setComponentConnection(long chatComponentId, @Nullable Long connectionId);

    Optional<Long> findChatComponentIdIgnoringConnection(
        long chatId, String componentName, int componentVersion, int environment);

    void removeTool(long chatToolId);

    void detachComponent(long chatComponentId);

    void deleteChatBindings(long chatId);

    List<AiHubChatToolBinding> listChatTools(long chatId);

    long attachUserComponent(
        long userId, long workspaceId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment);

    List<AiHubChatComponent> listUserComponents(long userId, long workspaceId);

    List<AiHubChatTool> listComponentTools(long chatComponentId);

    List<AiHubChatToolBinding> listUserTools(long userId, long workspaceId);

    void setComponentEnabled(long chatComponentId, boolean enabled);

    void setChatConnectorEnabled(long chatId, long userConnectorId, boolean enabled);

    Set<String> listChatDisabledConnectors(long chatId);

    void setToolEnabled(long chatComponentId, String toolName, boolean enabled);

    void setToolParameters(long chatComponentId, String toolName, Map<String, ?> parameters);

    void setToolRequiresApproval(long chatToolId, boolean requiresApproval);

    void setToolRequiresApproval(long chatComponentId, String toolName, boolean requiresApproval);
}
