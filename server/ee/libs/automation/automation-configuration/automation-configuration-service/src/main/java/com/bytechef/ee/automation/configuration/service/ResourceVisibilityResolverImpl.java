/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * EE visibility resolution, in order: admin bypass, reach, ownership, then resource grants.
 *
 * <p>
 * The first three are answered from the candidate records themselves, without touching the database. Only candidates
 * they leave undecided — private resources belonging to someone else — reach the grant table, and they go in one
 * batched query rather than one per row. A list where everything is workspace-visible, which is the common case, issues
 * no grant query at all.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class ResourceVisibilityResolverImpl implements ResourceVisibilityResolver {

    private final CurrentUserResolver currentUserResolver;
    private final ResourceGrantService resourceGrantService;

    @SuppressFBWarnings("EI")
    public ResourceVisibilityResolverImpl(
        CurrentUserResolver currentUserResolver, ResourceGrantService resourceGrantService) {

        this.currentUserResolver = currentUserResolver;
        this.resourceGrantService = resourceGrantService;
    }

    @Override
    public Set<Long> filterVisibleIds(
        String resourceType, long workspaceId, Collection<VisibilityRecord> candidates) {

        boolean admin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        Set<Long> visibleIds = new LinkedHashSet<>();
        List<Long> undecidedIds = new ArrayList<>();

        for (VisibilityRecord candidate : candidates) {
            ResourceVisibility visibility = candidate.visibility();

            if (admin || visibility.isAtLeast(ResourceVisibility.WORKSPACE) ||
                Objects.equals(currentUserLogin, candidate.createdBy())) {

                visibleIds.add(candidate.id());
            } else {
                undecidedIds.add(candidate.id());
            }
        }

        if (undecidedIds.isEmpty()) {
            return visibleIds;
        }

        OptionalLong currentUserId = currentUserResolver.fetchCurrentUserId();

        if (currentUserId.isEmpty()) {
            // Fail closed. Without a resolvable user there is no grant to look up, and treating that as "allow"
            // would widen access precisely when we know least about the caller.
            return visibleIds;
        }

        visibleIds.addAll(
            resourceGrantService.filterGrantedResourceIds(resourceType, currentUserId.getAsLong(), undecidedIds));

        return visibleIds;
    }
}
