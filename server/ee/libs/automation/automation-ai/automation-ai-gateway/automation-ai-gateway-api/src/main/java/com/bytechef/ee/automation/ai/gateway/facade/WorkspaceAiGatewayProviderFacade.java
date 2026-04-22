/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.dto.ProviderConnectionResult;
import java.util.List;

/**
 * Defines the interface for managing and retrieving AI LLM Gateway providers associated with specific workspaces. This
 * facade abstracts the details of underlying services and provides a streamlined way to access provider information
 * tied to a workspace.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProviderFacade {

    /**
     * Creates a new AI LLM Gateway provider and assigns it to the specified workspace.
     *
     * @param name        the name of the provider
     * @param type        the type of the provider
     * @param apiKey      the API key for the provider
     * @param baseUrl     the base URL for the provider
     * @param config      the configuration for the provider
     * @param workspaceId the workspace ID to assign the provider to
     * @return the created provider
     */
    AiGatewayProvider createWorkspaceProvider(
        String name, AiGatewayProviderType type, String apiKey, String baseUrl, String config, Long workspaceId);

    /**
     * Deletes an AI LLM Gateway provider, removing all workspace associations and the provider entity itself.
     *
     * @param workspaceId the workspace ID that owns the provider
     * @param providerId  the ID of the provider to delete
     */
    void deleteWorkspaceProvider(Long workspaceId, Long providerId);

    /**
     * Retrieves a list of AI LLM Gateway providers associated with the specified workspace.
     *
     * @param workspaceId the unique identifier of the workspace for which the providers are to be retrieved
     * @return a list of {@code AiGatewayProvider} objects associated with the workspace
     */
    List<AiGatewayProvider> getWorkspaceProviders(Long workspaceId);

    /**
     * Updates an existing AI LLM Gateway provider. Provider type is immutable and cannot be changed after creation.
     *
     * @param workspaceId the workspace ID that owns the provider
     * @param id          the ID of the provider to update
     * @param name        the new name
     * @param apiKey      the new API key (null to keep existing)
     * @param baseUrl     the new base URL
     * @param enabled     whether the provider is enabled (null to keep existing)
     * @param config      the new configuration
     * @return the updated provider
     */
    AiGatewayProvider updateWorkspaceProvider(
        Long workspaceId, Long id, String name, String apiKey, String baseUrl, Boolean enabled, String config);

    /**
     * Probes a provider's connectivity using its stored credentials. Returns a structured result instead of throwing so
     * the UI can render a specific error message rather than a generic 500.
     */
    ProviderConnectionResult testWorkspaceProviderConnection(Long workspaceId, Long providerId);
}
