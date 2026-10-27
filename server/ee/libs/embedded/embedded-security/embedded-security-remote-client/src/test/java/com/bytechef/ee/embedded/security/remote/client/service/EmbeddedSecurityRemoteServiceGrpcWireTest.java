/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.security.remote.grpc.JwtTokenRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.security.remote.grpc.SigningKeyRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class EmbeddedSecurityRemoteServiceGrpcWireTest {

    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private ManagedChannel managedChannel;
    private PublicKey publicKey;
    private RemoteJwtTokenServiceClient remoteJwtTokenServiceClient;
    private RemoteSigningKeyServiceClient remoteSigningKeyServiceClient;
    private Server server;
    private final SigningKeyService signingKeyService = mock(SigningKeyService.class);

    @BeforeEach
    void beforeEach() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        publicKey = keyPairGenerator.generateKeyPair()
            .getPublic();

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new JwtTokenRemoteServiceGrpcServer(jwtTokenService))
            .addService(new SigningKeyRemoteServiceGrpcServer(signingKeyService))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteJwtTokenServiceClient = new RemoteJwtTokenServiceClient(fixedGrpcChannels);
        remoteSigningKeyServiceClient = new RemoteSigningKeyServiceClient(fixedGrpcChannels);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testSigningKeyPublicKeyRoundTrip() {
        when(signingKeyService.getPublicKey("kid-1", 2L)).thenReturn(publicKey);

        assertThat(remoteSigningKeyServiceClient.getPublicKey("kid-1", 2L)
            .getEncoded()).isEqualTo(publicKey.getEncoded());
    }

    @Test
    void testUnknownSigningKeyFailsWithItsMessage() {
        when(signingKeyService.getPublicKey("kid-x", 2L)).thenThrow(
            new IllegalArgumentException("Signing key not found for keyId: kid-x"));

        assertThatThrownBy(() -> remoteSigningKeyServiceClient.getPublicKey("kid-x", 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Signing key not found");
    }

    @Test
    void testJwtPublicKeyRoundTrip() {
        when(jwtTokenService.getPublicKey("kid-1")).thenReturn(publicKey);

        assertThat(remoteJwtTokenServiceClient.getPublicKey("kid-1")
            .getEncoded()).isEqualTo(publicKey.getEncoded());
    }

    @Test
    void testMissingJwtPublicKeyIsNull() {
        when(jwtTokenService.getPublicKey("kid-1")).thenReturn(null);

        assertThat(remoteJwtTokenServiceClient.getPublicKey("kid-1")).isNull();
    }
}
