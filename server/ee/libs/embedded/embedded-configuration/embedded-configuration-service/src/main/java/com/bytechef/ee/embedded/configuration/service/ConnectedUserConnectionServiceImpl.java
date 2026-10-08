/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserSharedConnection;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserSharedConnectionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
public class ConnectedUserConnectionServiceImpl implements ConnectedUserConnectionService {

    private final ConnectedUserConnectionRepository connectedUserConnectionRepository;
    private final ConnectedUserSharedConnectionRepository connectedUserSharedConnectionRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionServiceImpl(
        ConnectedUserConnectionRepository connectedUserConnectionRepository,
        ConnectedUserSharedConnectionRepository connectedUserSharedConnectionRepository) {

        this.connectedUserConnectionRepository = connectedUserConnectionRepository;
        this.connectedUserSharedConnectionRepository = connectedUserSharedConnectionRepository;
    }

    @Override
    public void create(long connectedUserId, long connectionId) {
        ConnectedUserConnection connectedUserConnection = new ConnectedUserConnection();

        connectedUserConnection.setConnectedUserId(connectedUserId);
        connectedUserConnection.setConnectionId(connectionId);

        connectedUserConnectionRepository.save(connectedUserConnection);
    }

    @Override
    public void deleteByConnectionId(long connectionId) {
        connectedUserConnectionRepository.deleteByConnectionId(connectionId);
        connectedUserSharedConnectionRepository.deleteByConnectionId(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getConnectionIds(long connectedUserId) {
        return connectedUserConnectionRepository.findAllByConnectedUserId(connectedUserId)
            .stream()
            .map(ConnectedUserConnection::getConnectionId)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getSharedConnectionIds() {
        return connectedUserSharedConnectionRepository.findAll()
            .stream()
            .map(ConnectedUserSharedConnection::getConnectionId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void updateShared(long connectionId, boolean shared) {
        boolean currentlyShared = connectedUserSharedConnectionRepository.existsByConnectionId(connectionId);

        if (shared && !currentlyShared) {
            connectedUserSharedConnectionRepository.save(new ConnectedUserSharedConnection(connectionId));
        } else if (!shared && currentlyShared) {
            connectedUserSharedConnectionRepository.deleteByConnectionId(connectionId);
        }
    }
}
