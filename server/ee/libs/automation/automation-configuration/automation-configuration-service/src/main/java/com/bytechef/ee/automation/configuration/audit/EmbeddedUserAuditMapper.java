/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps {@code EmbeddedUserFacadeImpl} calls to audit data.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedUserAuditMapper implements AuditMapper {

    private final EmbeddedUserRepository embeddedUserRepository;

    @SuppressFBWarnings("EI")
    public EmbeddedUserAuditMapper(EmbeddedUserRepository embeddedUserRepository) {
        this.embeddedUserRepository = embeddedUserRepository;
    }

    @Override
    public Set<String> events() {
        return Set.of(
            EmbeddedUserAuditEvents.EMBEDDED_USER_ADDED,
            EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED,
            EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED,
            EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED,
            EmbeddedUserAuditEvents.EMBEDDED_USER_ROLE_UPDATED);
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        if (EmbeddedUserAuditEvents.EMBEDDED_USER_ADDED.equals(auditInvocation.event())) {
            return null;
        }

        long userId = (Long) auditInvocation.requireArgument("userId");

        return switch (auditInvocation.event()) {
            case EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED -> AuditRoles.describeAllEmbedded(
                embeddedUserRepository.findAllByUserId(userId));
            case EmbeddedUserAuditEvents.EMBEDDED_USER_ROLE_UPDATED -> embeddedUserRepository
                .findByUserIdAndEnvironmentIsNull(userId)
                .map(AuditRoles::describe)
                .orElse(null);
            case EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED -> fetchEnvironmentOrWideRow(
                userId, (Environment) auditInvocation.requireArgument("environment"))
                    .map(AuditRoles::describe)
                    .orElse(null);
            case EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED -> captureEnvironmentRemoval(
                userId, (Environment) auditInvocation.requireArgument("environment"));
            default -> null;
        };
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        Object userId = resolveUserId(auditInvocation);

        if (userId != null) {
            data.put("userId", String.valueOf(userId));
        }

        Object environment = auditInvocation.argument("environment");

        if (environment != null) {
            data.put("environment", ((Environment) environment).name());
        }

        WorkspaceRole embeddedRole = (WorkspaceRole) auditInvocation.argument("embeddedRole");

        if (embeddedRole != null) {
            data.put("role", embeddedRole.name());
        }

        Object captured = auditInvocation.captured();

        if (captured instanceof EnvironmentRemoval environmentRemoval) {
            if (environmentRemoval.previousRole() != null) {
                data.put("previousRole", environmentRemoval.previousRole());
            }

            data.put("widenedToEmbeddedWide", String.valueOf(environmentRemoval.widenedToEmbeddedWide()));
        } else if (captured != null) {
            data.put("previousRole", String.valueOf(captured));
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        if (!EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED.equals(auditInvocation.event())
            || auditInvocation.outcome() != AuditOutcome.SUCCESS) {

            return true;
        }

        return auditInvocation.captured() instanceof String previousRoles && !previousRoles.isEmpty();
    }

    private static @Nullable Object resolveUserId(AuditInvocation auditInvocation) {
        if (auditInvocation.arguments()
            .containsKey("userId")) {

            return auditInvocation.requireArgument("userId");
        }

        if (auditInvocation.result() instanceof EmbeddedUser embeddedUser) {
            return embeddedUser.getUserId();
        }

        return null;
    }

    private EnvironmentRemoval captureEnvironmentRemoval(long userId, Environment environment) {
        String previousRole = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal())
            .map(AuditRoles::describe)
            .orElse(null);
        List<EmbeddedUser> embeddedUsers = embeddedUserRepository.findAllByUserId(userId);

        return new EnvironmentRemoval(previousRole, previousRole != null && embeddedUsers.size() <= 1);
    }

    private Optional<EmbeddedUser> fetchEnvironmentOrWideRow(long userId, Environment environment) {
        Optional<EmbeddedUser> environmentEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal());

        if (environmentEmbeddedUser.isPresent()) {
            return environmentEmbeddedUser;
        }

        return embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId);
    }

    private record EnvironmentRemoval(@Nullable String previousRole, boolean widenedToEmbeddedWide) {
    }
}
