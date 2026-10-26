/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class PromotionPreviews {

    private PromotionPreviews() {
    }

    static List<PromotionConnectionMapping> connectionMappings(
        List<SourceBinding> sourceBindings, Map<Long, Long> existingTargetBindings, Map<Long, Long> suggestedMappings,
        ConnectionService connectionService) {

        return connectionMappings(
            sourceBindings, existingTargetBindings, suggestedMappings, Map.of(), connectionService);
    }

    static List<PromotionConnectionMapping> connectionMappings(
        List<SourceBinding> sourceBindings, Map<Long, Long> existingTargetBindings, Map<Long, Long> suggestedMappings,
        Map<Long, List<String>> extraUsages, ConnectionService connectionService) {

        Map<Long, Set<String>> usagesByConnectionId = new LinkedHashMap<>();

        for (SourceBinding sourceBinding : sourceBindings) {
            Set<String> usages =
                usagesByConnectionId.computeIfAbsent(sourceBinding.connectionId(), id -> new LinkedHashSet<>());

            usages.add(sourceBinding.workflowLabel() + " › " + sourceBinding.nodeName());
        }

        for (Map.Entry<Long, List<String>> extraUsage : extraUsages.entrySet()) {
            Set<String> usages = usagesByConnectionId.computeIfAbsent(extraUsage.getKey(), id -> new LinkedHashSet<>());

            usages.addAll(extraUsage.getValue());
        }

        if (usagesByConnectionId.isEmpty()) {
            return List.of();
        }

        List<Long> connectionIds = usagesByConnectionId.keySet()
            .stream()
            .sorted()
            .toList();

        List<PromotionConnectionMapping> promotionConnectionMappings = new ArrayList<>();

        for (Connection connection : connectionService.getConnections(connectionIds)) {
            long connectionId = connection.getId();

            Collection<String> usedBy = usagesByConnectionId.getOrDefault(connectionId, Set.of());

            promotionConnectionMappings.add(
                new PromotionConnectionMapping(
                    connectionId, connection.getName(), connection.getComponentName(),
                    connection.getConnectionVersion(),
                    existingTargetBindings.getOrDefault(connectionId, suggestedMappings.get(connectionId)),
                    List.copyOf(usedBy)));
        }

        return promotionConnectionMappings;
    }
}
