/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.config;

import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
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
public class TestResourcePromotionContributor implements EnvironmentPromotionContributor {

    public static final String RESOURCE_TYPE = "TEST_RESOURCE";
    public static final long TARGET_ID = 77L;

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    public EnvironmentPromotionPreview preview(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        return new EnvironmentPromotionPreview(
            RESOURCE_TYPE, sourceId, sourceEnvironment, targetEnvironment, null, null, List.of(), List.of(), List.of(),
            List.of(), List.of(), PromotionChangeHashes.destructiveChangesHash(List.of()));
    }

    @Override
    public EnvironmentPromotionResult promote(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
        Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

        return new EnvironmentPromotionResult(TARGET_ID, true, null, List.of());
    }
}
