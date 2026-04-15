/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service("permissionService")
@ConditionalOnEEVersion
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private final CurrentUserResolver currentUserResolver;
    private final ProjectUserRepository projectUserRepository;
    private final ProjectScopeCacheService projectScopeCacheService;
    private final WorkspaceUserRepository workspaceUserRepository;
    private final Map<String, Counter> checkErrorCounters;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionServiceImpl(
        CurrentUserResolver currentUserResolver, ProjectUserRepository projectUserRepository,
        ProjectScopeCacheService projectScopeCacheService, WorkspaceUserRepository workspaceUserRepository,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.currentUserResolver = currentUserResolver;
        this.projectUserRepository = projectUserRepository;
        this.projectScopeCacheService = projectScopeCacheService;
        this.workspaceUserRepository = workspaceUserRepository;

        // Lightweight EE apps (e.g., runtime-job-app) can start without actuator wired; ObjectProvider tolerates that.
        // A non-empty map ensures counter lookups are lock-free on the hot path.
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();

        this.checkErrorCounters = meterRegistry == null
            ? Collections.emptyMap()
            : Map.of(
                "hasWorkspaceRole", createCheckErrorCounter(meterRegistry, "hasWorkspaceRole"),
                "hasProjectScope", createCheckErrorCounter(meterRegistry, "hasProjectScope"),
                "hasProjectRole", createCheckErrorCounter(meterRegistry, "hasProjectRole"),
                "getMyProjectScopes", createCheckErrorCounter(meterRegistry, "getMyProjectScopes"),
                "getMyWorkspaceRole", createCheckErrorCounter(meterRegistry, "getMyWorkspaceRole"));
    }

    private static Counter createCheckErrorCounter(MeterRegistry meterRegistry, String method) {
        return Counter.builder("bytechef_permission_check_error")
            .description(
                "Number of permission checks that failed because the underlying repository / cache threw. "
                    + "Distinct from bytechef_permission_audit_failure: this counter fires BEFORE the audit row "
                    + "is written and isolates DB / cache outages from audit-write outages.")
            .tag("method", method)
            .register(meterRegistry);
    }

    /**
     * Wraps a DB-touching check, increments the per-method error counter on {@link DataAccessException}, and rethrows
     * so the audit aspect still records the frame and clients still see the failure as HTTP 500. Returning fail-closed
     * on DB failure would mask outages (user would see "no access" toast rather than an investigable error) and destroy
     * the fail-closed-vs-unavailable distinction operators rely on.
     */
    private <T> T withCheckErrorCounter(String methodName, Supplier<T> check) {
        try {
            return check.get();
        } catch (DataAccessException exception) {
            @Nullable
            Counter counter = checkErrorCounters.get(methodName);

            if (counter != null) {
                counter.increment();
            }

            logger.error(
                "PERMISSION_CHECK_ERROR[{}]: repository/cache failure on permission check. "
                    + "Request will surface as HTTP 500; see bytechef_permission_check_error{{method=\"{}\"}}.",
                methodName, methodName, exception);

            throw exception;
        }
    }

    @Override
    public boolean isTenantAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }

    @Override
    public boolean isCurrentUser(long userId) {
        OptionalLong currentUserId = currentUserResolver.fetchCurrentUserId();

        return currentUserId.isPresent() && currentUserId.getAsLong() == userId;
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        if (isTenantAdmin()) {
            return true;
        }

        Optional<WorkspaceRole> minimum = parseWorkspaceRole(minimumRole);

        if (minimum.isEmpty()) {
            return false;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        return withCheckErrorCounter("hasWorkspaceRole",
            () -> workspaceUserRepository.findByUserIdAndWorkspaceId(userId.getAsLong(), workspaceId)
                .flatMap(member -> toWorkspaceRole(member.getWorkspaceRole()))
                .map(role -> role.hasAtLeast(minimum.get()))
                .orElse(false));
    }

    @Override
    public boolean hasProjectScope(long projectId, String scope) {
        if (isTenantAdmin()) {
            return true;
        }

        Optional<PermissionScope> required = parsePermissionScope(scope);

        if (required.isEmpty()) {
            return false;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<PermissionScope> scopes = withCheckErrorCounter("hasProjectScope",
            () -> projectScopeCacheService.getProjectScopes(userId.getAsLong(), projectId));

        return scopes.contains(required.get());
    }

    @Override
    public boolean hasProjectRole(long projectId, String minimumRole) {
        if (isTenantAdmin()) {
            return true;
        }

        Optional<ProjectRole> minimum = parseProjectRole(minimumRole);

        if (minimum.isEmpty()) {
            return false;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Optional<ProjectUser> projectUser = withCheckErrorCounter("hasProjectRole",
            () -> projectUserRepository.findByProjectIdAndUserId(projectId, userId.getAsLong()));

        if (projectUser.isEmpty()) {
            return false;
        }

        // Built-in role path only. Compare by privilegeRank (not ordinal) so a future role appended to the end of
        // ProjectRole can still sit anywhere in the hierarchy.
        //
        // Custom roles are INTENTIONALLY excluded from the hierarchy check. The prior implementation treated any
        // custom role granting PROJECT_MANAGE_USERS as effective ADMIN here, which gave custom-role-only members a
        // self-promotion path: they could call addProjectUser(..., self, ADMIN) and have this method return true
        // because their scope bag contained PROJECT_MANAGE_USERS. Keeping the hierarchy check strictly built-in
        // means custom-role holders cannot grant or demote built-in roles at all (they can still manage other
        // custom-role memberships through separate flows). Orphaning protection (isEffectiveAdmin /
        // validateNotLastEffectiveAdmin) continues to count custom-role admins via PROJECT_MANAGE_USERS — that path
        // has the opposite requirement (be lenient about who counts as an admin when deciding removal safety).
        return toProjectRole(projectUser.get()
            .getProjectRole())
                .map(callerRole -> callerRole.hasAtLeast(minimum.get()))
                .orElse(false);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Set<String> getMyProjectScopes(long projectId) {
        if (isTenantAdmin()) {
            return EnumSet.allOf(PermissionScope.class)
                .stream()
                .map(PermissionScope::name)
                .collect(Collectors.toSet());
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return Collections.emptySet();
        }

        return withCheckErrorCounter("getMyProjectScopes",
            () -> projectScopeCacheService.getProjectScopes(userId.getAsLong(), projectId)
                .stream()
                .map(PermissionScope::name)
                .collect(Collectors.toSet()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public String getMyWorkspaceRole(long workspaceId) {
        if (isTenantAdmin()) {
            return WorkspaceRole.ADMIN.name();
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return null;
        }

        return withCheckErrorCounter("getMyWorkspaceRole",
            () -> workspaceUserRepository.findByUserIdAndWorkspaceId(userId.getAsLong(), workspaceId)
                .flatMap(member -> toWorkspaceRole(member.getWorkspaceRole()))
                .map(WorkspaceRole::name)
                .orElse(null));
    }

    @Override
    public void evictProjectScopeCache(long userId, long projectId) {
        projectScopeCacheService.evictProjectScopeCache(userId, projectId);
    }

    @Override
    public void evictProjectScopeCaches(Collection<UserProjectPair> userProjectPairs) {
        projectScopeCacheService.evictProjectScopeCaches(userProjectPairs);
    }

    @Override
    public void evictAllProjectScopeCache() {
        projectScopeCacheService.evictAllProjectScopeCache();
    }

    /**
     * Parses a {@link WorkspaceRole} name from an {@code @PreAuthorize} SpEL literal. A typo in the annotation produces
     * an ERROR log and a fail-closed {@code false} result rather than bubbling {@link IllegalArgumentException} out as
     * HTTP 500 (which audits as {@code ERROR}, not {@code DENIED}, and confuses operators).
     */
    private static Optional<WorkspaceRole> parseWorkspaceRole(String roleName) {
        try {
            return Optional.of(WorkspaceRole.valueOf(roleName));
        } catch (IllegalArgumentException exception) {
            logger.error("Unknown WorkspaceRole '{}' in @PreAuthorize — failing closed.", roleName);

            return Optional.empty();
        }
    }

    private static Optional<ProjectRole> parseProjectRole(String roleName) {
        try {
            return Optional.of(ProjectRole.valueOf(roleName));
        } catch (IllegalArgumentException exception) {
            logger.error("Unknown ProjectRole '{}' in @PreAuthorize — failing closed.", roleName);

            return Optional.empty();
        }
    }

    private static Optional<PermissionScope> parsePermissionScope(String scopeName) {
        try {
            return Optional.of(PermissionScope.valueOf(scopeName));
        } catch (IllegalArgumentException exception) {
            logger.error("Unknown PermissionScope '{}' in @PreAuthorize — failing closed.", scopeName);

            return Optional.empty();
        }
    }

    /**
     * Converts a stored {@code workspace_user.workspace_role} ordinal to a {@link WorkspaceRole}. Returns
     * {@link Optional#empty()} for null or out-of-range values instead of throwing
     * {@link ArrayIndexOutOfBoundsException}, so callers can fail closed on corrupted rows (typically a row written by
     * a newer binary and read by an older one, or manual SQL tampering).
     */
    private static Optional<WorkspaceRole> toWorkspaceRole(Integer ordinal) {
        if (ordinal == null) {
            return Optional.empty();
        }

        WorkspaceRole[] values = WorkspaceRole.values();

        if (ordinal < 0 || ordinal >= values.length) {
            logger.error(
                "workspace_user.workspace_role ordinal {} is out of range [0,{}) — failing closed. "
                    + "This indicates corrupted state or a forward/backward compatibility skew.",
                ordinal, values.length);

            return Optional.empty();
        }

        return Optional.of(values[ordinal]);
    }

    /**
     * Converts a stored {@code project_user.project_role} ordinal to a {@link ProjectRole}. Returns
     * {@link Optional#empty()} for null (custom-role membership) or out-of-range values instead of throwing
     * {@link ArrayIndexOutOfBoundsException}.
     */
    private static Optional<ProjectRole> toProjectRole(Integer ordinal) {
        if (ordinal == null) {
            return Optional.empty();
        }

        ProjectRole[] values = ProjectRole.values();

        if (ordinal < 0 || ordinal >= values.length) {
            logger.error(
                "project_user.project_role ordinal {} is out of range [0,{}) — failing closed. "
                    + "This indicates corrupted state or a forward/backward compatibility skew.",
                ordinal, values.length);

            return Optional.empty();
        }

        return Optional.of(values[ordinal]);
    }
}
