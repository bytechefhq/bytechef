/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.ee.ai.hub.security.WorkspaceAccessGuard;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AiHubTaskArtifactFacade}. Carries the {@code ADMIN} guard and the workspace access check,
 * and delegates to {@link AiHubTaskArtifactService} for the audit listing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
@Transactional(readOnly = true)
public class AiHubTaskArtifactFacadeImpl implements AiHubTaskArtifactFacade {

    /**
     * Default page size for the audit listing endpoint.
     */
    private static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * Hard upper bound on the {@code size} query parameter. Without this cap a caller could pass a huge {@code size}
     * and force the database to materialise an enormous result set.
     */
    private static final int MAX_PAGE_SIZE = 500;

    /**
     * Hard upper bound on the {@code page} query parameter. Bounded at 10_000 so a pathological
     * {@code page=999_999, size=500} request does not force PostgreSQL to walk hundreds of millions of index entries
     * via OFFSET-based pagination.
     */
    private static final int MAX_PAGE_INDEX = 10_000;

    private final AiHubTaskArtifactService taskArtifactService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiHubTaskArtifactFacadeImpl(
        AiHubTaskArtifactService taskArtifactService, UserService userService, WorkspaceFacade workspaceFacade) {

        this.taskArtifactService = taskArtifactService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiHubTaskArtifactPage getAiHubTaskArtifacts(
        long workspaceId, @Nullable Integer environment, @Nullable Long userId, @Nullable AiHubTaskArtifactKind kind,
        @Nullable Long from, @Nullable Long to, @Nullable Integer page, @Nullable Integer size) {

        long callerUserId = userService.getCurrentUser()
            .getId();

        WorkspaceAccessGuard.verifyUserCanAccessWorkspace(workspaceFacade, callerUserId, workspaceId);

        int requestedPage = page == null ? 0 : page;
        int requestedSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (requestedPage < 0 || requestedSize < 1) {
            // Negative page or zero/negative size are malformed — not "too large" — so they remain hard errors.
            // The clamp policy below covers the "out of range" cases (page > MAX_PAGE_INDEX, size > MAX_PAGE_SIZE).
            throw new IllegalArgumentException(
                "page must be >= 0 and size must be >= 1");
        }

        boolean pageClamped = requestedPage > MAX_PAGE_INDEX;
        boolean sizeClamped = requestedSize > MAX_PAGE_SIZE;
        int boundedPage = pageClamped ? MAX_PAGE_INDEX : requestedPage;
        int boundedSize = sizeClamped ? MAX_PAGE_SIZE : requestedSize;

        LocalDateTime fromLdt = from == null ? null : LocalDateTime.ofEpochSecond(from / 1000, 0, ZoneOffset.UTC);
        LocalDateTime toLdt = to == null ? null : LocalDateTime.ofEpochSecond(to / 1000, 0, ZoneOffset.UTC);

        List<AiHubTaskArtifact> items = taskArtifactService.listByWorkspace(
            workspaceId, environment, userId, kind, fromLdt, toLdt, boundedPage, boundedSize);

        long totalCount = taskArtifactService.countByWorkspace(
            workspaceId, environment, userId, kind, fromLdt, toLdt);

        boolean hasMore = ((long) boundedPage + 1) * boundedSize < totalCount;

        return new AiHubTaskArtifactPage(items, totalCount, hasMore, pageClamped, sizeClamped);
    }
}
