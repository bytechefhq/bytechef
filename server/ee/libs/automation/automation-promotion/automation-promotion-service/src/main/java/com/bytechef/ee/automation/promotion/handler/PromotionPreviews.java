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
 * Builds the connection section of an {@link com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview},
 * shared by every {@link EnvironmentPromotionHandler} so the promotion dialog renders one row per source connection
 * with identical semantics whichever resource type is being promoted.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class PromotionPreviews {

    private PromotionPreviews() {
    }

    /**
     * @see #connectionMappings(List, Map, Map, Map, ConnectionService)
     */
    static List<PromotionConnectionMapping> connectionMappings(
        List<SourceBinding> sourceBindings, Map<Long, Long> existingTargetBindings, Map<Long, Long> suggestedMappings,
        ConnectionService connectionService) {

        return connectionMappings(
            sourceBindings, existingTargetBindings, suggestedMappings, Map.of(), connectionService);
    }

    /**
     * Groups {@code sourceBindings} by connection id, loads every referenced connection in ONE
     * {@link ConnectionService#getConnections(List)} call rather than per binding, and pre-selects a target for each:
     * what the target environment already bound outranks a name-match suggestion, so a re-promotion never proposes
     * re-pointing a connection an operator deliberately wired.
     *
     * @param existingTargetBindings source connection id to the target connection already bound there, from
     *                               {@link com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter#existingTargetBindings};
     *                               empty when the target does not exist yet
     * @param suggestedMappings      source connection id to a guessed target, from
     *                               {@link com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper#suggest}
     * @param extraUsages            additional {@code usedBy} entries per source connection, for resources that bind
     *                               connections outside a deployment's workflow nodes (an MCP server's components, for
     *                               example) and therefore have usages no {@link SourceBinding} describes
     * @return one entry per distinct source connection, ordered by connection id
     */
    static List<PromotionConnectionMapping> connectionMappings(
        List<SourceBinding> sourceBindings, Map<Long, Long> existingTargetBindings, Map<Long, Long> suggestedMappings,
        Map<Long, List<String>> extraUsages, ConnectionService connectionService) {

        Map<Long, Set<String>> usagesByConnectionId = new LinkedHashMap<>();

        for (SourceBinding sourceBinding : sourceBindings) {
            Set<String> usages =
                usagesByConnectionId.computeIfAbsent(sourceBinding.connectionId(), id -> new LinkedHashSet<>());

            // A LinkedHashSet, not a list: one source connection bound at the same node under two connection keys
            // would otherwise be listed twice under an identical label.
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
