/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.platform.user.remote.client.config.PlatformUserRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.user.remote.grpc.AuthorityRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformUserRemoteClientIntTestConfiguration.class, properties = "spring.liquibase.contexts=user")
class RemoteAuthorityServiceClientIntTest {

    @Autowired
    private AuthorityService authorityService;

    private final List<Long> createdAuthorityIds = new ArrayList<>();
    private ManagedChannel managedChannel;
    private RemoteAuthorityServiceClient remoteAuthorityServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new AuthorityRemoteServiceGrpcServer(authorityService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteAuthorityServiceClient = new RemoteAuthorityServiceClient(new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        for (Long createdAuthorityId : createdAuthorityIds) {
            authorityService.delete(createdAuthorityId);
        }

        createdAuthorityIds.clear();
    }

    @Test
    void testFetchAuthorityReturnsTheStoredAuthority() {
        Authority authority = createAuthority("ROLE_AUDITOR");

        assertThat(remoteAuthorityServiceClient.fetchAuthority(authority.getId()))
            .hasValueSatisfying(fetchedAuthority -> {
                assertThat(fetchedAuthority.getId()).isEqualTo(authority.getId());
                assertThat(fetchedAuthority.getName()).isEqualTo("ROLE_AUDITOR");
            });
    }

    @Test
    void testFetchAuthorityReturnsEmptyWhenMissing() {
        assertThat(remoteAuthorityServiceClient.fetchAuthority(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testGetAuthoritiesReturnsEveryStoredAuthority() {
        Authority authority = createAuthority("ROLE_AUDITOR");

        List<Authority> storedAuthorities = authorityService.getAuthorities();

        List<Authority> authorities = remoteAuthorityServiceClient.getAuthorities();

        assertThat(authorities)
            .extracting(Authority::getId, Authority::getName)
            .containsExactlyElementsOf(
                storedAuthorities.stream()
                    .map(storedAuthority -> tuple(storedAuthority.getId(), storedAuthority.getName()))
                    .toList())
            .contains(tuple(authority.getId(), "ROLE_AUDITOR"));
        assertThat(authorities)
            .extracting(Authority::getName)
            .contains("ROLE_ADMIN", "ROLE_USER");
    }

    private Authority createAuthority(String name) {
        Authority authority = new Authority();

        authority.setName(name);

        authority = authorityService.create(authority);

        createdAuthorityIds.add(authority.getId());

        return authority;
    }
}
