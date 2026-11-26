/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion;

import com.bytechef.ee.automation.promotion.dto.PromotionConnectionCandidate;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedConnectionCandidates {

    private static final Comparator<PromotionConnectionCandidate> CANDIDATE_COMPARATOR = Comparator
        .comparing(PromotionConnectionCandidate::name)
        .thenComparingLong(PromotionConnectionCandidate::connectionId);

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectionService connectionService;

    @SuppressFBWarnings("EI")
    public EmbeddedConnectionCandidates(
        ConnectedUserConnectionMembership connectedUserConnectionMembership, ConnectionService connectionService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectionService = connectionService;
    }

    public List<PromotionConnectionCandidate> candidates(String componentName, Environment targetEnvironment) {
        List<Connection> connections = connectionService.getConnections(
            componentName, null, null, (long) targetEnvironment.ordinal(), PlatformType.EMBEDDED);

        Set<Long> connectionIds = connections.stream()
            .map(Connection::getId)
            .collect(Collectors.toSet());

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(
            connectionIds);

        return connections.stream()
            .filter(connection -> !ownedConnectionIds.contains(connection.getId()))
            .map(connection -> new PromotionConnectionCandidate(
                Objects.requireNonNull(connection.getId()), connection.getName()))
            .sorted(CANDIDATE_COMPARATOR)
            .toList();
    }
}
