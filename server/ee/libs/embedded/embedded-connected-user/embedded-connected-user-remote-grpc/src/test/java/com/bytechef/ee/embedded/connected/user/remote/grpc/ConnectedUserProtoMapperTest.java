/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ConnectedUserProtoMapperTest {

    @Test
    void testConnectedUserRoundTrip() {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), "ext@example.com", true, "ext-user", 9L, "Ext", 3);

        connectedUser.setCreatedBy("admin@localhost.com");
        connectedUser.setEnvironment(Environment.STAGING);
        connectedUser.setMetadata(Map.of("plan", "pro", "region", "eu"));

        ConnectedUser mappedConnectedUser = ConnectedUserProtoMapper.toConnectedUser(
            ConnectedUserProtoMapper.toRemoteConnectedUser(connectedUser));

        assertThat(mappedConnectedUser.getCreatedBy()).isEqualTo("admin@localhost.com");
        assertThat(mappedConnectedUser.getEmail()).isEqualTo("ext@example.com");
        assertThat(mappedConnectedUser.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(mappedConnectedUser.getExternalId()).isEqualTo("ext-user");
        assertThat(mappedConnectedUser.getId()).isEqualTo(9L);
        assertThat(mappedConnectedUser.getMetadata()).isEqualTo(Map.of("plan", "pro", "region", "eu"));
        assertThat(mappedConnectedUser.getName()).isEqualTo("Ext");
        assertThat(mappedConnectedUser.getVersion()).isEqualTo(3);
        assertThat(mappedConnectedUser.isEnabled()).isTrue();
    }

    @Test
    void testConnectedUserWithNullFieldsRoundTrip() {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, false, "ext-user", null, null, 0);

        ConnectedUser mappedConnectedUser = ConnectedUserProtoMapper.toConnectedUser(
            ConnectedUserProtoMapper.toRemoteConnectedUser(connectedUser));

        assertThat(mappedConnectedUser.getEmail()).isNull();
        assertThat(mappedConnectedUser.getId()).isNull();
        assertThat(mappedConnectedUser.getName()).isNull();
        assertThat(mappedConnectedUser.isEnabled()).isFalse();
    }
}
