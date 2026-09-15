/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@Transactional(readOnly = true)
public class CustomRoleScopeResolverImpl implements CustomRoleScopeResolver {

    private static final Logger log = LoggerFactory.getLogger(CustomRoleScopeResolverImpl.class);

    private static final long LOG_SUPPRESSION_WINDOW_MS = Duration.ofMinutes(5)
        .toMillis();
    private static final int LOG_SUPPRESSION_MAX_IDS = 10_000;

    private final CustomRoleRepository customRoleRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;

    /**
     * Per-{@code customRoleId} last-log timestamps for rate-limiting the orphan ERROR and the unknown-scope WARN. This
     * resolver is reached from inside {@link WorkspaceScopeCacheService#getWorkspaceScopes(long, long)}, whose
     * {@code @Cacheable} caches the result only until the next miss, and misses are not rare: the whole cache is
     * dropped on every custom-role scope edit, per member on every membership change, and on its own TTL, and each
     * distinct {@code (userId, workspaceId)} key referencing the same role misses separately. Without the rate limit a
     * single broken row logs at request rate. The maps are bounded: when one hits the cap it is cleared wholesale.
     */
    private final Map<Long, Long> lastOrphanLogTimestamps = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastUnknownScopeLogTimestamps = new ConcurrentHashMap<>();

    public CustomRoleScopeResolverImpl(
        CustomRoleRepository customRoleRepository, PermissionScopeRegistry permissionScopeRegistry) {

        this.customRoleRepository = customRoleRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
    }

    @Override
    public Optional<Set<String>> resolveScopes(long customRoleId) {
        Optional<CustomRole> customRole = customRoleRepository.findById(customRoleId);

        if (customRole.isEmpty()) {
            // Orphaned workspace_user reference to a non-existent custom role. ERROR (not WARN) because this is data
            // corruption — every scope check for the affected member will come back empty and the user may be
            // invisibly locked out of the workspace. Operators should alert on this log line.
            if (shouldLog(lastOrphanLogTimestamps, customRoleId)) {
                log.error(
                    "ORPHAN CUSTOM ROLE REFERENCE: custom_role_id={} does not exist. Every permission check for "
                        + "workspace_user rows referencing this id will fail closed. Investigate data integrity. "
                        + "(Further occurrences for the same id are suppressed for {} minutes.)",
                    customRoleId, Duration.ofMillis(LOG_SUPPRESSION_WINDOW_MS)
                        .toMinutes());
            }

            return Optional.empty();
        }

        CustomRole existingCustomRole = customRole.get();

        Set<String> knownScopeNames = permissionScopeRegistry.getAllScopeNames();
        Set<String> scopeNames = existingCustomRole.getScopeNames();

        Set<String> unknownScopeNames = scopeNames.stream()
            .filter(scopeName -> !knownScopeNames.contains(scopeName))
            .collect(Collectors.toCollection(TreeSet::new));

        // A scope a module no longer declares, after a rename or a removal. It grants nothing either way, but without
        // this line a member who lost access that way leaves nothing to look at.
        if (!unknownScopeNames.isEmpty() && shouldLog(lastUnknownScopeLogTimestamps, customRoleId)) {
            log.warn(
                "Custom role custom_role_id={} ({}) holds scopes no module declares, which grant nothing: {}. Edit the "
                    + "role to remove them. (Further occurrences for the same id are suppressed for {} minutes.)",
                customRoleId, existingCustomRole.getName(), unknownScopeNames,
                Duration.ofMillis(LOG_SUPPRESSION_WINDOW_MS)
                    .toMinutes());
        }

        return Optional.of(
            scopeNames.stream()
                .filter(knownScopeNames::contains)
                .collect(Collectors.toUnmodifiableSet()));
    }

    private static boolean shouldLog(Map<Long, Long> lastLogTimestamps, long customRoleId) {
        long now = System.currentTimeMillis();

        if (lastLogTimestamps.size() >= LOG_SUPPRESSION_MAX_IDS) {
            lastLogTimestamps.clear();
        }

        Long previous = lastLogTimestamps.get(customRoleId);

        if (previous != null && now - previous < LOG_SUPPRESSION_WINDOW_MS) {
            return false;
        }

        lastLogTimestamps.put(customRoleId, now);

        return true;
    }
}
