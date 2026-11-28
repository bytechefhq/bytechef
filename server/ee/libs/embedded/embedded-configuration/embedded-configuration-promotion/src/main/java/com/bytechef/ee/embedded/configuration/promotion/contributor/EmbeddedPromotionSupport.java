/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.contributor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.automation.promotion.connection.PromotionConnectionCandidates;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.embedded.configuration.promotion.EmbeddedConnectionCandidates;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class EmbeddedPromotionSupport {

    private EmbeddedPromotionSupport() {
    }

    static void checkMappedConnectionsAreCandidates(
        Map<Long, Long> connectionMappings, Environment targetEnvironment, ConnectionService connectionService,
        EmbeddedConnectionCandidates embeddedConnectionCandidates) {

        if (connectionMappings.isEmpty()) {
            return;
        }

        List<Connection> sourceConnections = connectionService.getConnections(
            new ArrayList<>(connectionMappings.keySet()));

        for (Connection sourceConnection : sourceConnections) {
            Long targetConnectionId = connectionMappings.get(sourceConnection.getId());

            if (targetConnectionId == null) {
                continue;
            }

            List<PromotionConnectionCandidate> candidates = embeddedConnectionCandidates.candidates(
                sourceConnection.getComponentName(), targetEnvironment);

            PromotionConnectionCandidates.checkIsCandidate(targetConnectionId, candidates);
        }
    }

    static @Nullable String internalOnlyInputWarning(
        Workflow workflow, boolean newToTarget, boolean enabled, @Nullable String missingInternalOnlyInputName,
        String environmentName) {

        if (!enabled || missingInternalOnlyInputName == null) {
            return null;
        }

        if (newToTarget) {
            return "Workflow \"%s\" is created disabled until its internal-only input \"%s\" is filled in %s."
                .formatted(workflowLabel(workflow), missingInternalOnlyInputName, environmentName);
        }

        return ("Workflow \"%s\" needs a value for its internal-only input \"%s\" in %s; the promotion is refused " +
            "until it is filled there.").formatted(
                workflowLabel(workflow), missingInternalOnlyInputName, environmentName);
    }

    static boolean isCreatedDisabled(
        boolean newToTarget, boolean enabled, @Nullable String missingInternalOnlyInputName) {

        return newToTarget && enabled && missingInternalOnlyInputName != null;
    }

    static Map<Long, Long> suggestedConnectionMappings(
        Collection<Long> sourceConnectionIds, Environment targetEnvironment, ConnectionService connectionService,
        EmbeddedConnectionCandidates embeddedConnectionCandidates) {

        if (sourceConnectionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> suggestedMappings = new HashMap<>();

        for (Connection sourceConnection : connectionService.getConnections(new ArrayList<>(sourceConnectionIds))) {
            List<PromotionConnectionCandidate> candidates = embeddedConnectionCandidates.candidates(
                sourceConnection.getComponentName(), targetEnvironment);

            Long suggestedConnectionId = PromotionConnectionCandidates.suggestByName(
                sourceConnection.getName(), candidates);

            if (suggestedConnectionId != null) {
                suggestedMappings.put(sourceConnection.getId(), suggestedConnectionId);
            }
        }

        return suggestedMappings;
    }

    static List<String> unresolvedConnectionWarnings(
        List<PromotionConnectionMapping> connectionMappings, String environmentName) {

        List<String> warnings = new ArrayList<>();

        for (PromotionConnectionMapping connectionMapping : connectionMappings) {
            if (connectionMapping.suggestedTargetConnectionId() == null) {
                warnings.add(
                    ("Connection \"%s\" has no counterpart in %s; the workflows that use it stay without a " +
                        "connection until one is chosen.").formatted(
                            connectionMapping.sourceConnectionName(), environmentName));
            }
        }

        return warnings;
    }

    static String workflowLabel(Workflow workflow) {
        String label = workflow.getLabel();

        return StringUtils.isBlank(label) ? workflow.getId() : label;
    }
}
