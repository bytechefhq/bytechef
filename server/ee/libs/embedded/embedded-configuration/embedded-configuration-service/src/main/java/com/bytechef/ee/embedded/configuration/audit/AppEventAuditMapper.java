/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.audit;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class AppEventAuditMapper implements AuditMapper {

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        return Map.of();
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        if (auditInvocation.outcome() != AuditOutcome.SUCCESS) {
            return true;
        }

        return !Boolean.FALSE.equals(auditInvocation.result());
    }
}
