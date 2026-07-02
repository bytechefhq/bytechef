/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserConnectionServiceTest {

    @Mock
    private ConnectedUserConnectionRepository connectedUserConnectionRepository;

    @Test
    void testCreate() {
        ConnectedUserConnectionService service =
            new ConnectedUserConnectionServiceImpl(connectedUserConnectionRepository);

        service.create(1L, 5L);

        ArgumentCaptor<ConnectedUserConnection> captor = ArgumentCaptor.forClass(ConnectedUserConnection.class);

        verify(connectedUserConnectionRepository).save(captor.capture());

        ConnectedUserConnection saved = captor.getValue();

        assertThat(saved.getConnectedUserId()).isEqualTo(1L);
        assertThat(saved.getConnectionId()).isEqualTo(5L);
    }

    @Test
    void testGetConnectionIds() {
        ConnectedUserConnectionService service =
            new ConnectedUserConnectionServiceImpl(connectedUserConnectionRepository);

        ConnectedUserConnection connection = new ConnectedUserConnection();

        connection.setConnectedUserId(1L);
        connection.setConnectionId(5L);

        when(connectedUserConnectionRepository.findAllByConnectedUserId(1L)).thenReturn(List.of(connection));

        assertThat(service.getConnectionIds(1L)).containsExactly(5L);
    }
}
