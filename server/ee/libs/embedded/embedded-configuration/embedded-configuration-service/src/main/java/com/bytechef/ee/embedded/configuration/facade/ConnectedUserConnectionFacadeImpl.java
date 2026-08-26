/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserConnectionFacadeImpl implements ConnectedUserConnectionFacade {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserConnectionFacadeImpl.class);

    private static final int MAX_WARNED_CONNECTED_USER_IDS = 1_000;

    private static final Set<Long> WARNED_CONNECTED_USER_IDS = ConcurrentHashMap.newKeySet();

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionService connectedUserConnectionService, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
    }

    @Override
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);

        return connectionId;
    }

    @Override
    public void deleteConnectedUserConnection(long connectedUserId, long connectionId) {
        requireOwned(connectedUserId, connectionId);

        connectionFacade.delete(connectionId);
    }

    /**
     * Returns the connections this connected user is entitled to: their own -- the connections behind their integration
     * instances plus the ones they created themselves -- and the connections bound at the configuration level of the
     * configurations those instances derive from, which is what a shared connection is in this data model.
     * {@link ConnectedUserConnectionMembership} computes all three, and it is the same computation
     * {@code ConnectedUserResourceMembershipResolver} authorizes against, so what this list shows and what a subsequent
     * request is granted cannot disagree.
     * <p>
     * {@code connectionIds} carries the host's {@code sharedConnectionIds}, which reach the server by way of the
     * browser -- declared over the {@code EMBED_INIT} postMessage handshake and forwarded as a request parameter. It is
     * a caller assertion the server cannot verify, and it used to be added to the result unconditionally, which let any
     * connected user read any embedded connection in the tenant by guessing its id. It is now ignored entirely: the
     * sharing it was meant to express is derived server-side instead, from configuration-level bindings the tenant
     * admin actually made.
     * <p>
     * Ids the entitlement does not cover are logged rather than rejected, so a host that declares a connection no
     * configuration binds is visible to an operator instead of silently losing it.
     */
    @Override
    public List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> entitledConnectionIds = connectedUserConnectionMembership.getConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        logUnentitledRequestedConnectionIds(connectedUserId, connectionIds, entitledConnectionIds);

        return connectionFacade.getConnections(new ArrayList<>(entitledConnectionIds), PlatformType.EMBEDDED)
            .stream()
            .filter(connectionDTO -> componentName == null || componentName.equals(connectionDTO.componentName()))
            .toList();
    }

    /**
     * Replaces the connection's authorization parameters wholesale: a parameter the caller does not resubmit is
     * cleared, not preserved. This deliberately differs from the merge this path used before — both the embedded hub
     * and the workspace surface now carry one semantics — and it is why the reconnect also marks the connection's
     * credentials valid again, which the merge-based path never did.
     */
    @Override
    public void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters) {
        requireOwned(connectedUserId, connectionId);

        connectionFacade.replaceAuthorizationParameters(connectionId, parameters);
    }

    /**
     * Entitlement is not ownership, and this check is the reason the difference matters. A configuration-level shared
     * connection is entitled to every connected user attached to that configuration, so it now appears in
     * {@link #getConnections}; deleting or reauthorizing it would act on every one of those users at once. The
     * connection belongs to the tenant admin who bound it, and only the admin surface may change it.
     */
    private void requireOwned(long connectedUserId, long connectionId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getOwnedConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        List<ConnectionDTO> ownedConnectionDTOs = connectionFacade.getConnections(
            new ArrayList<>(ownedConnectionIds), PlatformType.EMBEDDED);

        boolean owned = ownedConnectionDTOs.stream()
            .anyMatch(connectionDTO -> Objects.equals(connectionDTO.id(), connectionId));

        if (!owned) {
            throw new NoSuchElementException("Connection id=%s not found".formatted(connectionId));
        }
    }

    /**
     * Reports whether this connected user still deserves a WARN. A host that uses shared connections sends them on
     * every connections fetch -- every editor mount, every hub view -- so warning per request would bury the signal it
     * exists to give. One line per connected user per JVM is enough to identify who depends on the parameter.
     * <p>
     * Once the tracker is full it goes quiet rather than warning unconditionally: a full tracker means the signal has
     * already been given many times over, and the alternative -- resuming per-request logging at exactly the moment the
     * set stops absorbing ids -- turns a bounded diagnostic into a log flood any caller can drive.
     */
    private static boolean shouldWarnFor(Long connectedUserId) {
        if (WARNED_CONNECTED_USER_IDS.size() >= MAX_WARNED_CONNECTED_USER_IDS) {
            return false;
        }

        return WARNED_CONNECTED_USER_IDS.add(connectedUserId);
    }

    /**
     * Logs the requested ids this connected user is not entitled to. Now that configuration-level bindings are derived
     * server-side, a declared id that still lands here is one no configuration this user is attached to binds -- which
     * is the residual worth an operator's attention: a host declaring a connection it was never actually given.
     */
    private static void logUnentitledRequestedConnectionIds(
        Long connectedUserId, List<Long> connectionIds, Set<Long> entitledConnectionIds) {

        if (connectionIds.isEmpty() || !log.isWarnEnabled()) {
            return;
        }

        List<Long> unentitledConnectionIds = connectionIds.stream()
            .filter(connectionId -> !entitledConnectionIds.contains(connectionId))
            .toList();

        if (!unentitledConnectionIds.isEmpty() && shouldWarnFor(connectedUserId)) {
            log.warn(
                "Ignored {} shared connection id(s) {} requested for connected user id={}: they are neither this " +
                    "user's own connections nor bound at any integration instance configuration this user has an " +
                    "instance for",
                unentitledConnectionIds.size(), unentitledConnectionIds, connectedUserId);
        }
    }
}
