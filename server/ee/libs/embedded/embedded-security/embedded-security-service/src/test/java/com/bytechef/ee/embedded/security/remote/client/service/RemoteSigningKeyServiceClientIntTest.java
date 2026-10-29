/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.security.config.EmbeddedSecurityIntTestConfiguration;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.remote.grpc.SigningKeyRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = EmbeddedSecurityIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/embedded-security-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    PermissionScopeRegistry.class, ProjectRepository.class, UserService.class, WorkspaceScopeCacheService.class,
    WorkspaceUserRepository.class
})
class RemoteSigningKeyServiceClientIntTest {

    private static final long USER_ID = 1001L;

    @Autowired
    private SigningKeyRepository signingKeyRepository;

    @Autowired
    private SigningKeyService signingKeyService;

    private ManagedChannel managedChannel;
    private RemoteSigningKeyServiceClient remoteSigningKeyServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new SigningKeyRemoteServiceGrpcServer(signingKeyService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteSigningKeyServiceClient = new RemoteSigningKeyServiceClient(new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        signingKeyRepository.deleteAll();
    }

    @Test
    void testGetPublicKeyReturnsTheStoredPublicKeyThatVerifiesTokensSignedWithItsPrivateKey() throws Exception {
        SigningKey signingKey = newSigningKey(Environment.PRODUCTION);

        PrivateKey privateKey = decodePrivateKey(signingKeyService.create(signingKey));

        PublicKey publicKey = remoteSigningKeyServiceClient.getPublicKey(
            signingKey.getKeyId(), Environment.PRODUCTION.ordinal());

        assertThat(publicKey.getEncoded()).isEqualTo(
            signingKeyService.getPublicKey(signingKey.getKeyId(), Environment.PRODUCTION.ordinal())
                .getEncoded());

        String jwtToken = Jwts.builder()
            .subject("customer-a")
            .signWith(privateKey)
            .compact();

        Claims claims = Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(jwtToken)
            .getPayload();

        assertThat(claims.getSubject()).isEqualTo("customer-a");
    }

    @Test
    void testGetPublicKeyOfAnotherEnvironmentFailsWithTheServiceMessage() {
        SigningKey signingKey = newSigningKey(Environment.STAGING);

        signingKeyService.create(signingKey);

        assertThatThrownBy(
            () -> remoteSigningKeyServiceClient.getPublicKey(signingKey.getKeyId(), Environment.PRODUCTION.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Signing key not found for keyId: " + signingKey.getKeyId());
    }

    @Test
    void testGetPublicKeyOfAnUnknownKeyIdFailsWithTheServiceMessage() {
        assertThatThrownBy(
            () -> remoteSigningKeyServiceClient.getPublicKey("unknown-key-id", Environment.PRODUCTION.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Signing key not found for keyId: unknown-key-id");
    }

    @Test
    void testGetPublicKeyIsReadFromTheServiceOnEveryCall() {
        SigningKey signingKey = newSigningKey(Environment.PRODUCTION);

        signingKeyService.create(signingKey);

        assertThat(remoteSigningKeyServiceClient.getPublicKey(signingKey.getKeyId(), Environment.PRODUCTION.ordinal()))
            .isNotNull();

        signingKeyService.delete(signingKey.getId());

        assertThatThrownBy(
            () -> remoteSigningKeyServiceClient.getPublicKey(signingKey.getKeyId(), Environment.PRODUCTION.ordinal()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static PrivateKey decodePrivateKey(String pemPrivateKey) throws Exception {
        String base64PrivateKey = pemPrivateKey.replaceAll("-----(BEGIN|END) PRIVATE KEY-----|\\s", "");

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(EncodingUtils.base64Decode(base64PrivateKey)));
    }

    private static SigningKey newSigningKey(Environment environment) {
        SigningKey signingKey = new SigningKey();

        signingKey.setEnvironment(environment);
        signingKey.setName("signing-key-" + UUID.randomUUID());
        signingKey.setType(PlatformType.EMBEDDED);
        signingKey.setUserId(USER_ID);

        return signingKey;
    }
}
