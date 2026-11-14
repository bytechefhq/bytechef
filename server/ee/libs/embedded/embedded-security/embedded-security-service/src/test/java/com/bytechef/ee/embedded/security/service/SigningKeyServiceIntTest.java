/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.security.config.EmbeddedSecurityIntTestConfiguration;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
class SigningKeyServiceIntTest {

    private static final long ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    @Autowired
    private SigningKeyRepository signingKeyRepository;

    @Autowired
    private SigningKeyService signingKeyService;

    @AfterEach
    void afterEach() {
        signingKeyRepository.deleteAll();
    }

    @Test
    void testUpdateLastUsedDateSetsTheDateOnFirstUse() {
        SigningKey signingKey = saveSigningKey(Environment.PRODUCTION);

        assertThat(signingKey.getLastUsedDate()).isNull();

        Instant beforeUse = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS);

        signingKeyService.updateLastUsedDate(signingKey.getKeyId(), ENVIRONMENT_ID);

        assertThat(getLastUsedDate(signingKey.getId())).isAfterOrEqualTo(beforeUse);
    }

    @Test
    void testUpdateLastUsedDateWithinTheIntervalKeepsTheDate() {
        SigningKey signingKey = saveSigningKey(Environment.PRODUCTION);

        signingKeyService.updateLastUsedDate(signingKey.getKeyId(), ENVIRONMENT_ID);

        Instant firstLastUsedDate = getLastUsedDate(signingKey.getId());

        signingKeyService.updateLastUsedDate(signingKey.getKeyId(), ENVIRONMENT_ID);

        assertThat(firstLastUsedDate).isNotNull();
        assertThat(getLastUsedDate(signingKey.getId())).isEqualTo(firstLastUsedDate);
    }

    @Test
    void testUpdateLastUsedDateAfterTheIntervalRefreshesTheDate() {
        SigningKey signingKey = saveSigningKey(Environment.PRODUCTION);

        Instant staleLastUsedDate = Instant.now()
            .minus(2, ChronoUnit.MINUTES)
            .truncatedTo(ChronoUnit.SECONDS);

        signingKey.setLastUsedDate(staleLastUsedDate);

        signingKeyRepository.save(signingKey);

        signingKeyService.updateLastUsedDate(signingKey.getKeyId(), ENVIRONMENT_ID);

        assertThat(getLastUsedDate(signingKey.getId())).isAfter(staleLastUsedDate.plus(1, ChronoUnit.MINUTES));
    }

    @Test
    void testUpdateLastUsedDateDoesNotChangeTheLastModifiedDate() {
        SigningKey signingKey = saveSigningKey(Environment.PRODUCTION);

        SigningKey savedSigningKey = signingKeyService.getSigningKey(signingKey.getId());

        Instant lastModifiedDate = savedSigningKey.getLastModifiedDate();

        signingKeyService.updateLastUsedDate(signingKey.getKeyId(), ENVIRONMENT_ID);

        SigningKey usedSigningKey = signingKeyService.getSigningKey(signingKey.getId());

        assertThat(usedSigningKey.getLastUsedDate()).isNotNull();
        assertThat(usedSigningKey.getLastModifiedDate()).isEqualTo(lastModifiedDate);
    }

    @Test
    void testUpdateLastUsedDateIgnoresAKeyOfAnotherEnvironment() {
        SigningKey signingKey = saveSigningKey(Environment.DEVELOPMENT);

        signingKeyService.updateLastUsedDate(signingKey.getKeyId(), ENVIRONMENT_ID);

        assertThat(getLastUsedDate(signingKey.getId())).isNull();
    }

    private Instant getLastUsedDate(long id) {
        SigningKey signingKey = signingKeyService.getSigningKey(id);

        return signingKey.getLastUsedDate();
    }

    private SigningKey saveSigningKey(Environment environment) {
        SigningKey signingKey = new SigningKey();

        signingKey.setEnvironment(environment);
        signingKey.setKeyId(String.valueOf(UUID.randomUUID()));
        signingKey.setName("signing-key-" + UUID.randomUUID());
        signingKey.setPublicKey("public-key");
        signingKey.setType(PlatformType.EMBEDDED);
        signingKey.setUserId(1001L);

        return signingKeyRepository.save(signingKey);
    }
}
