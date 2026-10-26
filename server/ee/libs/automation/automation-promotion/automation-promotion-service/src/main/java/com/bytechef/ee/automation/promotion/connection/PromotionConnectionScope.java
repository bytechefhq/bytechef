/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PromotionConnectionScope {

    private PromotionConnectionScope() {
    }

    public static Set<Long> sourceConnectionIds(List<SourceBinding> sourceBindings) {
        Set<Long> sourceConnectionIds = new LinkedHashSet<>();

        for (SourceBinding sourceBinding : sourceBindings) {
            sourceConnectionIds.add(sourceBinding.connectionId());
        }

        return sourceConnectionIds;
    }

    public static void checkMappedConnectionsBelongToSource(
        Set<Long> sourceConnectionIds, Map<Long, Long> requestedMappings) {

        for (Long mappedSourceConnectionId : requestedMappings.keySet()) {
            if (!sourceConnectionIds.contains(mappedSourceConnectionId)) {
                throw new ConfigurationException(
                    "Connection mapping references connection %s, which the promoted resource does not use"
                        .formatted(mappedSourceConnectionId),
                    EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID);
            }
        }
    }
}
