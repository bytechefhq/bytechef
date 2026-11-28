/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.audit;

import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EnvironmentPromotionAuditMapper implements AuditMapper {

    private final Map<String, PlatformType> platformTypesByResourceType;

    public EnvironmentPromotionAuditMapper(List<EnvironmentPromotionContributor> contributors) {
        Map<String, PlatformType> platformTypesByResourceType = new HashMap<>();

        for (EnvironmentPromotionContributor contributor : contributors) {
            platformTypesByResourceType.put(contributor.getResourceType(), contributor.getPlatformType());
        }

        this.platformTypesByResourceType = Map.copyOf(platformTypesByResourceType);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        return Map.of();
    }

    @Override
    public @Nullable PlatformType platform(AuditInvocation auditInvocation) {
        if (auditInvocation.argument("resourceType") instanceof String resourceType) {
            return platformTypesByResourceType.get(resourceType);
        }

        return null;
    }
}
