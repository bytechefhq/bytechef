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
 * Parses the {@code X-ByteChef-Workspace-Id} header and verifies the authenticated caller belongs to the claimed
 * workspace. Returns {@code null} for missing/non-numeric headers (controller then returns 400); throws
 * {@link AiScoreWorkspaceBoundaryException} (mapped to 403 by the exception handler) when the caller is not a member of
 * the claimed workspace.
 *
 * <p>
 * <b>Security note:</b> without this check, any authenticated API key holder could write to arbitrary workspaces by
 * lying about the header value. This is a band-aid — the proper fix is to add a {@code workspace_id} column to
 * {@code ApiKey} and eliminate the header entirely.
 *
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

    /**
     * Parses the header and verifies the authenticated caller belongs to the claimed workspace.
     *
     * @return the parsed + authorized workspace id, or {@code null} if the header is missing/non-numeric
     * @throws AiScoreWorkspaceBoundaryException if the authenticated user is not a member of the claimed workspace
     */
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

        // Workspace ids are positive longs by convention (BIGINT autoIncrement starts at >0). Treat
        // <= 0 the same as a non-numeric header — return null so the controller responds 400, instead of
        // wasting a workspaceUserService DB lookup that we already know will return empty.
        if (claimedWorkspaceId <= 0) {
            return null;
        }

        Long callerUserId = resolveCallerUserId(claimedWorkspaceId);

        if (callerUserId == null) {
            // No authenticated user in context — the auth filter should have rejected earlier. Defensive: treat as a
            // boundary violation rather than silently allow the request through. Diagnostic detail goes to the WARN
            // log (server-side only); the wire response uses the uniform message so a probing caller cannot
            // distinguish "not authenticated" from "not a member" by reading the 403 body.
            log.warn(
                "AI gateway boundary check: no authenticated user resolved for claimed workspace {}",
                claimedWorkspaceId);

            throw AiScoreWorkspaceBoundaryException.forTarget(
                claimedWorkspaceId, AiScoreTargetType.WORKSPACE, claimedWorkspaceId);
        }

        boolean member = workspaceUserService.fetchWorkspaceUser(callerUserId, claimedWorkspaceId)
            .isPresent();

        if (!member) {
            // The internal user PK must NOT leak to the wire — a probing caller learning their own PK is
            // an information-disclosure risk, and the precise "User X is not a member of workspace Y" wording
            // also tells them which membership-check path failed. Log structurally; throw the uniform message.
            log.warn(
                "AI gateway boundary check: user {} is not a member of claimed workspace {}",
                callerUserId, claimedWorkspaceId);

            throw AiScoreWorkspaceBoundaryException.forTarget(
                claimedWorkspaceId, AiScoreTargetType.WORKSPACE, claimedWorkspaceId);
        }

        return claimedWorkspaceId;
    }

    /**
     * Resolves the authenticated user's id from the current {@link AiGatewayApiKeyAuthenticationToken}. Returns
     * {@code null} when no AI gateway API-key token is present (e.g. anonymous, or some other token type on a dev
     * profile).
     *
     * <p>
     * If the API-key id from the token corresponds to a revoked or rotated key, surfaces as
     * {@link AiScoreWorkspaceBoundaryException} (mapped to 403). Narrowed to {@link IllegalArgumentException} (the
     * documented "not found" path) — letting an {@code IllegalArgumentException} from the {@link ApiKeyService}
     * propagate would be mapped to 400 by the generic exception handler, hiding a security-relevant condition under a
     * malformed-input status. Other {@code RuntimeException}s (notably {@code DataAccessException} for a transient DB
     * failure / pool exhaustion) propagate so the gateway's exception handler can map them to 503 Service Unavailable
     * rather than misleading the client with a 403 "revoked or rotated" body.
     */
    private Long resolveCallerUserId(Long claimedWorkspaceId) {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (!(authentication instanceof AiGatewayApiKeyAuthenticationToken token) || token.getApiKeyId() == null) {
            return null;
        }

        Long apiKeyId = token.getApiKeyId();

        try {
            ApiKey apiKey = apiKeyService.getApiKey(apiKeyId);

            return apiKey.getUserId();
        } catch (IllegalArgumentException notFound) {
            // The apiKeyId is internal — surfacing it (or the "revoked or rotated" precise reason) to the
            // wire would help a probing caller enumerate key state. The WARN log retains both for ops.
            log.warn(
                "API-key lookup returned not-found for apiKeyId {} during workspace-header resolution: {}",
                apiKeyId, notFound.getMessage());

            throw AiScoreWorkspaceBoundaryException.forTarget(
                claimedWorkspaceId, AiScoreTargetType.WORKSPACE, claimedWorkspaceId, notFound);
        }
    }
}
