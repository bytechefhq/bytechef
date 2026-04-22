/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository interface for managing {@link WorkspaceAiGatewayProvider} entities.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceAiGatewayProviderRepository
    extends ListCrudRepository<WorkspaceAiGatewayProvider, Long> {

    /**
     * Find all workspace AI LLM Gateway provider relationships by workspace ID.
     *
     * @param workspaceId the workspace ID
     * @return list of workspace AI LLM Gateway provider relationships
     */
    List<WorkspaceAiGatewayProvider> findAllByWorkspaceId(Long workspaceId);

    /**
     * Find all workspace AI LLM Gateway provider relationships by provider ID.
     *
     * @param providerId the provider ID
     * @return list of workspace AI LLM Gateway provider relationships
     */
    List<WorkspaceAiGatewayProvider> findByProviderId(Long providerId);

    /**
     * Find workspace AI LLM Gateway provider relationship by workspace ID and provider ID.
     *
     * @param workspaceId the workspace ID
     * @param providerId  the provider ID
     * @return workspace AI LLM Gateway provider relationship if found
     */
    WorkspaceAiGatewayProvider findByWorkspaceIdAndProviderId(Long workspaceId, Long providerId);
}
