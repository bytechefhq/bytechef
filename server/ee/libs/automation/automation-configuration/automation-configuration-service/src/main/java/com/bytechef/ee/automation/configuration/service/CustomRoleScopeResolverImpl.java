/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.CustomRoleScope;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private static final Logger logger = LoggerFactory.getLogger(CustomRoleScopeResolverImpl.class);

    private static final long LOG_SUPPRESSION_WINDOW_MS = Duration.ofMinutes(5)
        .toMillis();
    private static final int LOG_SUPPRESSION_MAX_IDS = 10_000;

    private final CustomRoleRepository customRoleRepository;

    /**
     * Per-{@code customRoleId} last-log timestamp for rate-limiting the ORPHAN ERROR. Without it, a single corrupted
     * row combined with the "empty result is never cached" rule on
     * {@link ProjectScopeCacheService#getProjectScopes(long, long)} would spam ERROR at request rate (one log line per
     * permission check). The map is bounded so a probing attacker cannot push it unbounded: when the size hits the cap
     * we clear it wholesale (simpler than LRU; the cost of re-logging an id once every reset is acceptable compared to
     * the operational pain of log spam).
     */
    private final Map<Long, Long> lastOrphanLogTimestamps = new ConcurrentHashMap<>();

    public CustomRoleScopeResolverImpl(CustomRoleRepository customRoleRepository) {
        this.customRoleRepository = customRoleRepository;
    }

    @Override
    public Optional<Set<PermissionScope>> resolveScopes(long customRoleId) {
        Optional<CustomRole> customRole = customRoleRepository.findById(customRoleId);

        if (customRole.isEmpty()) {
            // Orphaned project_user reference to a non-existent custom role. ERROR (not WARN) because this is data
            // corruption — every scope check for the affected member will come back empty and the user may be
            // invisibly locked out of the project. Operators should alert on this log line. Rate-limited per id so a
            // single corrupted row does not log at request rate.
            if (shouldLogOrphan(customRoleId)) {
                logger.error(
                    "ORPHAN CUSTOM ROLE REFERENCE: custom_role_id={} does not exist. Every permission check for "
                        + "project_user rows referencing this id will fail closed. Investigate data integrity. "
                        + "(Further occurrences for the same id are suppressed for {} minutes.)",
                    customRoleId, Duration.ofMillis(LOG_SUPPRESSION_WINDOW_MS)
                        .toMinutes());
            }

            return Optional.empty();
        }

        return customRole.map(role -> role.getScopes()
            .stream()
            .map(CustomRoleScope::scope)
            .collect(Collectors.toUnmodifiableSet()));
    }

    private boolean shouldLogOrphan(long customRoleId) {
        long now = System.currentTimeMillis();

        if (lastOrphanLogTimestamps.size() >= LOG_SUPPRESSION_MAX_IDS) {
            lastOrphanLogTimestamps.clear();
        }

        Long previous = lastOrphanLogTimestamps.get(customRoleId);

        if (previous != null && now - previous < LOG_SUPPRESSION_WINDOW_MS) {
            return false;
        }

        lastOrphanLogTimestamps.put(customRoleId, now);

        return true;
    }
}
