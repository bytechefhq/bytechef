/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Maps {@code CustomRoleServiceImpl} writes. The free-text description is not recorded.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CustomRoleAuditMapper implements AuditMapper {

    private final CustomRoleRepository customRoleRepository;

    @SuppressFBWarnings("EI")
    public CustomRoleAuditMapper(CustomRoleRepository customRoleRepository) {
        this.customRoleRepository = customRoleRepository;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        if (CustomRoleAuditEvents.CUSTOM_ROLE_CREATED.equals(auditInvocation.event())) {
            return null;
        }

        return customRoleRepository.findById((Long) auditInvocation.argument("roleId"))
            .map(customRole -> new PreviousCustomRole(customRole.getName(), sorted(customRole.getScopeNames())))
            .orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        Object customRoleId = auditInvocation.argument("roleId");

        if (customRoleId == null && auditInvocation.result() instanceof CustomRole customRole) {
            customRoleId = customRole.getId();
        }

        if (customRoleId != null) {
            data.put("customRoleId", String.valueOf(customRoleId));
        }

        Object name = auditInvocation.argument("name");

        if (name != null) {
            data.put("name", String.valueOf(name));
        }

        if (auditInvocation.argument("scopeNames") instanceof Collection<?> scopeNames) {
            List<String> sortedScopeNames = sorted(scopeNames);

            data.put("scopes", AuditValues.joinBounded(sortedScopeNames));
            data.put("scopeCount", String.valueOf(sortedScopeNames.size()));
        }

        if (auditInvocation.captured() instanceof PreviousCustomRole previousCustomRole) {
            if (CustomRoleAuditEvents.CUSTOM_ROLE_DELETED.equals(auditInvocation.event())) {
                data.put("name", previousCustomRole.name());
            } else {
                data.put("previousName", previousCustomRole.name());
                data.put("previousScopes", AuditValues.joinBounded(previousCustomRole.scopes()));
                data.put("previousScopeCount", String.valueOf(previousCustomRole.scopes()
                    .size()));
            }
        }

        return data;
    }

    private static List<String> sorted(Collection<?> values) {
        return values.stream()
            .map(String::valueOf)
            .sorted()
            .toList();
    }

    private record PreviousCustomRole(String name, List<String> scopes) {
    }
}
