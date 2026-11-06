/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.workspace;

import com.bytechef.ee.automation.ai.gateway.security.web.authentication.AiGatewayApiKeyAuthenticationToken;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayWorkspaceHeaderResolver {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayWorkspaceHeaderResolver.class);

    private final ApiKeyService apiKeyService;
    private final WorkspaceUserService workspaceUserService;

    public AiGatewayWorkspaceHeaderResolver(ApiKeyService apiKeyService, WorkspaceUserService workspaceUserService) {
        this.apiKeyService = apiKeyService;
        this.workspaceUserService = workspaceUserService;
    }

    public Long resolveAndVerify(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }

        Long claimedWorkspaceId;

        try {
            claimedWorkspaceId = Long.parseLong(header.trim());
        } catch (NumberFormatException numberFormatException) {
            return null;
        }

        if (claimedWorkspaceId <= 0) {
            return null;
        }

        Long callerUserId = resolveCallerUserId(claimedWorkspaceId);

        if (callerUserId == null) {
            log.warn(
                "AI gateway boundary check: no authenticated user resolved for claimed workspace {}",
                claimedWorkspaceId);

            throw AiScoreWorkspaceBoundaryException.forTarget(
                claimedWorkspaceId, AiScoreTargetType.WORKSPACE, claimedWorkspaceId);
        }

        boolean member = workspaceUserService.fetchWorkspaceUser(callerUserId, claimedWorkspaceId)
            .isPresent();

        if (!member) {
            log.warn(
                "AI gateway boundary check: user {} is not a member of claimed workspace {}",
                callerUserId, claimedWorkspaceId);

            throw AiScoreWorkspaceBoundaryException.forTarget(
                claimedWorkspaceId, AiScoreTargetType.WORKSPACE, claimedWorkspaceId);
        }

        return claimedWorkspaceId;
    }

    private Long resolveCallerUserId(Long claimedWorkspaceId) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (!(authentication instanceof AiGatewayApiKeyAuthenticationToken token)) {
            return null;
        }

        Long apiKeyId = token.getApiKeyId();

        if (apiKeyId == null) {
            return null;
        }

        try {
            ApiKey apiKey = apiKeyService.getApiKey(apiKeyId);

            return apiKey.getUserId();
        } catch (IllegalArgumentException notFound) {
            log.warn(
                "API-key lookup returned not-found for apiKeyId {} during workspace-header resolution: {}",
                apiKeyId, notFound.getMessage());

            throw AiScoreWorkspaceBoundaryException.forTarget(
                claimedWorkspaceId, AiScoreTargetType.WORKSPACE, claimedWorkspaceId, notFound);
        }
    }
}
