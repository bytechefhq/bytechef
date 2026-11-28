/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnvironmentPromotionAuditMapperTest {

    private final EnvironmentPromotionAuditMapper environmentPromotionAuditMapper =
        new EnvironmentPromotionAuditMapper(
            List.of(new StubContributor("DEPLOYMENT", null),
                new StubContributor("EMBEDDED_THING", PlatformType.EMBEDDED)));

    @Test
    void testPlatformDefaultsToAutomation() {
        assertThat(environmentPromotionAuditMapper.platform(invocation("DEPLOYMENT")))
            .isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testPlatformFollowsTheContributorOfTheResourceType() {
        assertThat(environmentPromotionAuditMapper.platform(invocation("EMBEDDED_THING")))
            .isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testPlatformOfUnknownResourceTypeIsNull() {
        assertThat(environmentPromotionAuditMapper.platform(invocation("UNKNOWN"))).isNull();
        assertThat(environmentPromotionAuditMapper.platform(invocation(null))).isNull();
    }

    @Test
    void testMapAddsNoData() {
        assertThat(environmentPromotionAuditMapper.map(invocation("DEPLOYMENT"))).isEmpty();
    }

    private static AuditInvocation invocation(@Nullable String resourceType) {
        Map<String, @Nullable Object> arguments = new HashMap<>();

        arguments.put("resourceType", resourceType);

        return new AuditInvocation(
            EnvironmentPromotionAuditEvents.ENVIRONMENT_PROMOTED, arguments, null, null, AuditOutcome.SUCCESS, null);
    }

    private record StubContributor(String resourceType, @Nullable PlatformType platformType)
        implements EnvironmentPromotionContributor {

        @Override
        public PlatformType getPlatformType() {
            return platformType == null ? PlatformType.AUTOMATION : platformType;
        }

        @Override
        public String getResourceType() {
            return resourceType;
        }

        @Override
        public EnvironmentPromotionPreview preview(
            long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

            throw new UnsupportedOperationException();
        }

        @Override
        public EnvironmentPromotionResult promote(
            long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
            Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

            throw new UnsupportedOperationException();
        }
    }
}
