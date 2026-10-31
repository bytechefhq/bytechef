/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.audit;

import com.bytechef.ee.platform.user.scim.model.ScimGroup;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ScimGroupAuditMapper implements AuditMapper {

    private final AuthorityService authorityService;

    @SuppressFBWarnings("EI")
    public ScimGroupAuditMapper(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        if (auditInvocation.event()
            .equals(ScimGroupAuditEvents.SCIM_GROUP_CREATED)) {

            return null;
        }

        long id = (Long) auditInvocation.requireArgument("id");

        return authorityService.fetchAuthority(id)
            .map(Authority::getName)
            .orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        Authority authority = toAuthority(auditInvocation.result());
        String previousName = auditInvocation.captured() instanceof String capturedName ? capturedName : null;

        if (auditInvocation.argument("id") instanceof Long id) {
            data.put("groupId", String.valueOf(id));
        } else if (authority != null && authority.getId() != null) {
            data.put("groupId", String.valueOf(authority.getId()));
        }

        String name;

        if (authority != null) {
            name = authority.getName();
        } else if (auditInvocation.argument("scimGroup") instanceof ScimGroup scimGroup) {
            name = scimGroup.displayName();
        } else {
            name = previousName;
        }

        if (name != null) {
            data.put("name", name);
        }

        if (authority != null && previousName != null && !Objects.equals(previousName, name)) {
            data.put("previousName", previousName);
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        if (auditInvocation.outcome() != AuditOutcome.SUCCESS) {
            return true;
        }

        Object result = auditInvocation.result();

        return !(result instanceof Optional<?> optional && optional.isEmpty()) && !Boolean.FALSE.equals(result);
    }

    private static @Nullable Authority toAuthority(@Nullable Object result) {
        Object value = result instanceof Optional<?> optional ? optional.orElse(null) : result;

        return value instanceof Authority authority ? authority : null;
    }
}
