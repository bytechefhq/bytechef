/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.assetfile.web.graphql;

import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Workspace-membership authorization for the asset file GraphQL surface. Mirrors the REST controller's posture: by-id
 * operations throw {@link AssetFileNotFoundException} (→ "not found") for both "file does not exist" and "file exists
 * in another workspace" so ids cannot be enumerated across workspaces, while operations that take an explicit
 * {@code workspaceId} throw {@link AccessDeniedException} because the caller is asserting membership of a workspace
 * they named themselves.
 *
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("EI2")
class AssetFileGraphQlAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(AssetFileGraphQlAccessGuard.class);

    private final AssetFileFacade assetFileFacade;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    AssetFileGraphQlAccessGuard(
        AssetFileFacade assetFileFacade, UserService userService, WorkspaceFacade workspaceFacade) {

        this.assetFileFacade = assetFileFacade;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    /**
     * Resolves the workspace owning {@code fileId} and verifies the calling user is a member. Returns the workspace id
     * so callers can avoid a second lookup.
     */
    long verifyFileAccess(Long fileId) {
        Long workspaceId = assetFileFacade.getOwningWorkspaceId(fileId);

        long userId = getCurrentUserId();

        if (!isUserWorkspaceMember(userId, workspaceId)) {
            log.warn(
                "AssetFileGraphQlController returning not-found (security-audit event): user {} attempted to access "
                    + "foreign asset file",
                userId);

            throw new AssetFileNotFoundException("Asset file not accessible");
        }

        return workspaceId;
    }

    /**
     * Verifies the calling user is a member of the explicitly supplied {@code workspaceId}.
     */
    void verifyWorkspaceAccess(long workspaceId) {
        long userId = getCurrentUserId();

        if (!isUserWorkspaceMember(userId, workspaceId)) {
            log.warn(
                "AssetFileGraphQlController denying access (security-audit event): user {} attempted to access "
                    + "workspace they are not a member of",
                userId);

            throw new AccessDeniedException("Workspace not accessible");
        }
    }

    private long getCurrentUserId() {
        return userService.getCurrentUser()
            .getId();
    }

    private boolean isUserWorkspaceMember(long userId, long workspaceId) {
        return workspaceFacade.getUserWorkspaces(userId)
            .stream()
            .map(Workspace::getId)
            .anyMatch(id -> id != null && id == workspaceId);
    }
}
