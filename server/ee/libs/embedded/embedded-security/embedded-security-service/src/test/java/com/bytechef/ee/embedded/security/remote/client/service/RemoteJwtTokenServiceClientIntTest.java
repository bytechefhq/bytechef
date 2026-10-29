/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.security.remote.grpc.JwtTokenRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.JwtTokenServiceImpl;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = RemoteJwtTokenServiceClientIntTest.Config.class, properties = "bytechef.edition=ee")
class RemoteJwtTokenServiceClientIntTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JwtTokenService jwtTokenService;

    private ManagedChannel managedChannel;
    private RemoteJwtTokenServiceClient remoteJwtTokenServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new JwtTokenRemoteServiceGrpcServer(jwtTokenService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteJwtTokenServiceClient = new RemoteJwtTokenServiceClient(new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        getPublicKeysCache().clear();
    }

    @Test
    void testGetPublicKeyReturnsTheKeyThatVerifiesATokenTheServiceGenerated() {
        String jwtToken = jwtTokenService.generateJwtToken("customer-a", 7, 1, "tenant-one");

        Claims claims = Jwts.parser()
            .keyLocator(header -> remoteJwtTokenServiceClient.getPublicKey((String) header.get("kid")))
            .build()
            .parseSignedClaims(jwtToken)
            .getPayload();

        assertThat(claims.getSubject()).isEqualTo("customer-a");
        assertThat(claims.get("integrationId", Long.class)).isEqualTo(7L);
    }

    @Test
    void testGetPublicKeyOfAKeyIdTheServiceNeverIssuedIsNull() {
        jwtTokenService.generateJwtToken("customer-a", 7, 1, "tenant-one");

        assertThat(remoteJwtTokenServiceClient.getPublicKey("unknown-key-id")).isNull();
    }

    @Test
    void testGetPublicKeyIsCachedPerKeyIdOnTheClient() {
        String keyId = getKeyId(jwtTokenService.generateJwtToken("customer-b", 7, 1, "tenant-two"));

        PublicKey publicKey = remoteJwtTokenServiceClient.getPublicKey(keyId);

        assertThat(publicKey.getEncoded()).isEqualTo(
            Objects.requireNonNull(jwtTokenService.getPublicKey(keyId))
                .getEncoded());

        Cache publicKeysCache = getPublicKeysCache();

        publicKeysCache.clear();

        assertThat(jwtTokenService.getPublicKey(keyId)).isNull();
        assertThat(remoteJwtTokenServiceClient.getPublicKey(keyId)
            .getEncoded()).isEqualTo(publicKey.getEncoded());
    }

    @Test
    void testMissingPublicKeyIsCachedPerKeyIdOnTheClient() {
        assertThat(remoteJwtTokenServiceClient.getPublicKey("late-key-id")).isNull();

        String keyId = getKeyId(jwtTokenService.generateJwtToken("customer-c", 7, 1, "tenant-three"));

        Cache publicKeysCache = getPublicKeysCache();

        publicKeysCache.put("late-key-id", Objects.requireNonNull(publicKeysCache.get(keyId, String.class)));

        assertThat(jwtTokenService.getPublicKey("late-key-id")).isNotNull();
        assertThat(remoteJwtTokenServiceClient.getPublicKey("late-key-id")).isNull();
    }

    private String getKeyId(String jwtToken) {
        AtomicReference<String> keyIdReference = new AtomicReference<>();

        Jwts.parser()
            .keyLocator(header -> {
                keyIdReference.set((String) header.get("kid"));

                return jwtTokenService.getPublicKey(keyIdReference.get());
            })
            .build()
            .parseSignedClaims(jwtToken);

        return keyIdReference.get();
    }

    private Cache getPublicKeysCache() {
        return Objects.requireNonNull(cacheManager.getCache(JwtTokenServiceImpl.PUBLIC_KEYS_CACHE));
    }

    @Import(JwtTokenServiceImpl.class)
    @SpringBootConfiguration
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }
}
