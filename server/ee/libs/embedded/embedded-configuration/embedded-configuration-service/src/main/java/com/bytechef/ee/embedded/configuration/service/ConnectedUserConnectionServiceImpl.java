/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionServiceImpl(ConnectedUserConnectionRepository connectedUserConnectionRepository) {
        this.connectedUserConnectionRepository = connectedUserConnectionRepository;
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
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getConnectionIds(long connectedUserId) {
        return connectedUserConnectionRepository.findAllByConnectedUserId(connectedUserId)
            .stream()
            .map(ConnectedUserConnection::getConnectionId)
            .toList();
    }
}
